param([Parameter(Mandatory)][string]$ImageTag)

$ErrorActionPreference = 'Stop'
$project = 'nite-staging-smoke-' + [guid]::NewGuid().ToString('N')
$composeFile = Join-Path $PSScriptRoot '../compose.staging.yaml'
$composeArgs = @('compose', '--env-file', ([System.IO.Path]::GetTempFileName()), '-p', $project, '-f', $composeFile)
$keys = @('STAGING_IMAGE_TAG', 'STAGING_API_PORT', 'DB_PASSWORD', 'ENTRA_TENANT_ID', 'ENTRA_AUDIENCE', 'ENTRA_REQUIRED_SCOPE')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }

function Invoke-Compose {
    param([string[]]$CommandArgs)
    $result = & docker @composeArgs @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        $detail = ($result | Select-Object -Last 40) -join [Environment]::NewLine
        throw "Docker Compose falhou: $($CommandArgs[0]) $detail"
    }
    return $result
}

try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $listener.Stop()
    $env:STAGING_IMAGE_TAG = $ImageTag
    $env:STAGING_API_PORT = [string]$port
    $env:DB_PASSWORD = [guid]::NewGuid().ToString('N')
    $env:ENTRA_TENANT_ID = '11111111-1111-4111-8111-111111111111'
    $env:ENTRA_AUDIENCE = '22222222-2222-4222-8222-222222222222'
    $env:ENTRA_REQUIRED_SCOPE = 'access_as_user'

    $config = (Invoke-Compose -CommandArgs @('config', '--format', 'json')) | ConvertFrom-Json
    if ($config.services.db.ports) { throw 'PostgreSQL tem porta publicada no host' }
    if ($config.services.api.ports[0].host_ip -ne '127.0.0.1') { throw 'API exposta fora de loopback' }
    $audience = $env:ENTRA_AUDIENCE
    $env:ENTRA_AUDIENCE = $null
    & docker @composeArgs config --quiet 2>$null | Out-Null
    $missingConfigExit = $LASTEXITCODE
    $env:ENTRA_AUDIENCE = $audience
    if ($missingConfigExit -eq 0) { throw 'Compose aceitou configuração Entra incompleta' }
    $imageUser = & docker image inspect "nite-connect-api:$ImageTag" --format '{{.Config.User}}'
    if ($LASTEXITCODE -ne 0 -or $imageUser -ne '10001:10001') { throw 'Imagem inexistente ou processo não configurado sem root' }
    Invoke-Compose -CommandArgs @('up', '-d', '--wait', 'db') | Out-Null
    Invoke-Compose -CommandArgs @('run', '--rm', '--no-deps',
        '-e', 'NITE_BOOTSTRAP_OBJECT_ID=33333333-3333-4333-8333-333333333333',
        '-e', 'NITE_BOOTSTRAP_NAME=Smoke Staging',
        '-e', "NITE_BOOTSTRAP_INSTITUTIONAL_EMAIL=$project@unijorge.com.br",
        '-e', 'NITE_BOOTSTRAP_REASON=Teste descartável de implantação',
        '-e', 'NITE_BOOTSTRAP_ROLE=GESTOR_GERAL',
        'api', '--spring.main.web-application-type=none', '--nite.bootstrap.enabled=true') | Out-Null
    Invoke-Compose -CommandArgs @('up', '-d', 'api') | Out-Null
    $ready = $false
    $deadline = [DateTime]::UtcNow.AddMinutes(2)
    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            $health = Invoke-RestMethod "http://127.0.0.1:$port/actuator/health" -TimeoutSec 3
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'API não ficou saudável em dois minutos' }
    $response = Invoke-WebRequest "http://127.0.0.1:$port/api/v1/projects" -SkipHttpErrorCheck -TimeoutSec 10
    if ($response.StatusCode -ne 401) { throw 'API não recusou chamada sem token com HTTP 401' }
    $versions = Invoke-Compose -CommandArgs @('exec', '-T', 'db', 'psql', '-U', 'niteconnect', '-d', 'niteconnect_staging', '-Atc', "select string_agg(version, ',' order by installed_rank) from public.flyway_schema_history where success and version is not null")
    if (($versions -join '').Trim() -ne '1,2,3') { throw 'Migrações V1–V3 não foram aplicadas' }
    Invoke-Compose -CommandArgs @('stop', 'api') | Out-Null
    Invoke-Compose -CommandArgs @('up', '-d', 'api') | Out-Null
    $deadline = [DateTime]::UtcNow.AddMinutes(2)
    $ready = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            $health = Invoke-RestMethod "http://127.0.0.1:$port/actuator/health" -TimeoutSec 3
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'API falhou após reinício com banco existente' }
    Write-Output 'PASS: Entra obrigatório, loopback, banco privado, usuário sem root, bootstrap, health, HTTP 401, V1–V3 e reinício.'
} finally {
    # Apenas recursos descartáveis deste projeto de teste, com nome único.
    & docker @composeArgs down --volumes --remove-orphans | Out-Null
    $cleanupExit = $LASTEXITCODE
    Remove-Item -LiteralPath $composeArgs[2] -ErrorAction SilentlyContinue
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    if ($cleanupExit -ne 0) { throw 'Falha ao remover os recursos descartáveis do smoke test' }
}
