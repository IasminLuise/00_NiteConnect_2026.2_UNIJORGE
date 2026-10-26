param([Parameter(Mandatory=$true)][string]$JobsFile)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.runtime'))+[IO.Path]::DirectorySeparatorChar
function PrivatePath([string]$path) {
    $full=[IO.Path]::GetFullPath($path)
    if(-not $full.StartsWith($root,[StringComparison]::OrdinalIgnoreCase)){throw 'Private copies and outputs required'}
    return $full
}
$jobs=Get-Content -Raw -LiteralPath (PrivatePath $JobsFile) | ConvertFrom-Json
$app=New-Object -ComObject Word.Application
$oldSecurity=$app.AutomationSecurity; $oldLinks=$app.Options.UpdateLinksAtOpen
$oldAlerts=$app.DisplayAlerts; $oldVisible=$app.Visible
$app.AutomationSecurity=3; $app.Options.UpdateLinksAtOpen=$false; $app.DisplayAlerts=0; $app.Visible=$false
try {
    foreach($job in $jobs){
        $source=PrivatePath $job.source; $pdf=PrivatePath $job.pdf; $json=PrivatePath $job.json
        if((Test-Path -LiteralPath $pdf) -or (Test-Path -LiteralPath $json)){throw 'Fresh outputs required'}
        $doc=$null
        try {
            $doc=$app.Documents.Open($source,$false,$true,$false)
            $fieldData=@()
            foreach($first in $doc.StoryRanges){
                $story=$first
                while($null -ne $story){
                    foreach($field in $story.Fields){
                        $entry=@{story=[int]$story.StoryType; type=[int]$field.Type; code=[string]$field.Code.Text; before=[string]$field.Result.Text}
                        # Only local date/reference fields; never update external-content fields.
                        if($job.update_fields -and ([int]$field.Type -in @(22,3))){$entry['updated']=[bool]$field.Update()}
                        $entry['after']=[string]$field.Result.Text; $fieldData += $entry
                    }
                    $story=$story.NextStoryRange
                }
            }
            $doc.Repaginate()
            $paragraphs=@(); $i=0
            foreach($p in $doc.Paragraphs){
                $i++; $start=$p.Range.Duplicate; $start.Collapse(1)
                $end=$p.Range.Duplicate; $end.Collapse(0)
                $paragraphs+=@{index=$i; startPage=[int]$start.Information(3); endPage=[int]$end.Information(3); fontSize=[double]$p.Range.Font.Size;
                    verticalPagePoints=[double]$start.Information(6); spaceBefore=[double]$p.Format.SpaceBefore;
                    spaceAfter=[double]$p.Format.SpaceAfter; lineSpacing=[double]$p.Format.LineSpacing; lineSpacingRule=[int]$p.Format.LineSpacingRule}
            }
            $doc.ExportAsFixedFormat($pdf,17)
            @{id=$job.id; wordVersion=$app.Version; wordBuild=$app.Build; pages=[int]$doc.ComputeStatistics(2); fields=$fieldData; paragraphs=$paragraphs} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $json -Encoding utf8
            Write-Output ('Word read-only probe complete: '+$job.id)
        } finally {if($null -ne $doc){$doc.Close(0)}}
    }
} finally {
    $app.AutomationSecurity=$oldSecurity; $app.Options.UpdateLinksAtOpen=$oldLinks
    $app.DisplayAlerts=$oldAlerts; $app.Visible=$oldVisible
    if($app.Documents.Count -eq 0){$app.Quit()}
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($app)
}
