param([Parameter(Mandatory=$true)][string]$JobsFile)
$ErrorActionPreference='Stop'
$jobsPath=(Resolve-Path -LiteralPath $JobsFile).Path
$runtimeRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.runtime'))+[IO.Path]::DirectorySeparatorChar
if(-not $jobsPath.StartsWith($runtimeRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Jobs must be private under .runtime'}
$jobs=Get-Content -Raw -LiteralPath $jobsPath | ConvertFrom-Json
$wordApp=New-Object -ComObject Word.Application
$oldVisible=$wordApp.Visible
$wordApp.Visible=$false
$oldSecurity=$wordApp.AutomationSecurity
$oldLinks=$wordApp.Options.UpdateLinksAtOpen
$oldAlerts=$wordApp.DisplayAlerts
$wordApp.AutomationSecurity=3
$wordApp.Options.UpdateLinksAtOpen=$false
$wordApp.DisplayAlerts=0
try {
    foreach($job in $jobs) {
        $source=[IO.Path]::GetFullPath($job.source)
        $pdf=[IO.Path]::GetFullPath($job.pdf)
        if(-not $source.StartsWith($runtimeRoot,[StringComparison]::OrdinalIgnoreCase) -or -not $pdf.StartsWith($runtimeRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Word input/output must be private copies'}
        $doc=$null
        try {
            $editable=$null -ne $job.output_docx
            $doc=$wordApp.Documents.Open($source,$false,(-not $editable),$false)
            if($editable){
                $target=[IO.Path]::GetFullPath($job.output_docx)
                if(-not $target.StartsWith($runtimeRoot,[StringComparison]::OrdinalIgnoreCase) -or $target -eq $source){throw 'Control must save to a distinct private file'}
                # Both engines fill exactly the same body cell; selection is explicit, not Ctrl+End.
                if($null -ne $job.paragraph){$range=$doc.Paragraphs.Item([int]$job.paragraph).Range.Duplicate}
                else {$cell=$doc.Tables.Item([int]$job.table).Cell([int]$job.row,[int]$job.column); $range=$cell.Range.Duplicate}
                $range.End=$range.End-1
                $range.Collapse(0)
                $range.InsertAfter([string]$job.append)
                $doc.SaveAs2($target,16)
            }
            $doc.Repaginate()
            $doc.ExportAsFixedFormat($pdf,17)
            Write-Output ('Word control exported: '+[string]$job.id)
        } finally {if($null -ne $doc){$doc.Close(0)}}
    }
} finally {
    $wordApp.Options.UpdateLinksAtOpen=$oldLinks
    $wordApp.AutomationSecurity=$oldSecurity
    $wordApp.DisplayAlerts=$oldAlerts
    $wordApp.Visible=$oldVisible
    if($wordApp.Documents.Count -eq 0){$wordApp.Quit()}
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($wordApp)
}
