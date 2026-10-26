param([Parameter(Mandatory=$true)][string]$Run)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.runtime'))+[IO.Path]::DirectorySeparatorChar
$directory=[IO.Path]::GetFullPath($Run)
if(-not $directory.StartsWith($root,[StringComparison]::OrdinalIgnoreCase)){throw 'Private runtime required'}
$source=Join-Path $directory 'active/fixtures/F01.docx'
$target=Join-Path $directory 'analysis/word-XYZ-control.docx'
if(Test-Path -LiteralPath $target){throw 'Fresh control required'}
$app=New-Object -ComObject Word.Application
$security=$app.AutomationSecurity; $links=$app.Options.UpdateLinksAtOpen; $alerts=$app.DisplayAlerts; $visible=$app.Visible
$app.AutomationSecurity=3; $app.Options.UpdateLinksAtOpen=$false; $app.DisplayAlerts=0; $app.Visible=$false
$doc=$null
try {
    Copy-Item -LiteralPath $source -Destination $target
    $doc=$app.Documents.Open($target,$false,$false,$false)
    $range=$doc.Range(0,0); $range.InsertBefore('XYZ'); $doc.Save()
    Write-Output 'Synthetic Word control saved independently with the same XYZ edit'
} finally {
    if($null -ne $doc){$doc.Close(0)}
    $app.AutomationSecurity=$security; $app.Options.UpdateLinksAtOpen=$links; $app.DisplayAlerts=$alerts; $app.Visible=$visible
    if($app.Documents.Count -eq 0){$app.Quit()}
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($app)
}
