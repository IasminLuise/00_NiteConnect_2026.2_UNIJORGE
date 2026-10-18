param([Parameter(Mandatory=$true)][string]$JobsFile)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../.private'))+[IO.Path]::DirectorySeparatorChar
function PrivatePath([string]$path){$full=[IO.Path]::GetFullPath($path);if(-not $full.StartsWith($root,[StringComparison]::OrdinalIgnoreCase)){throw 'Private copies required'};return $full}
$jobs=Get-Content -Raw -LiteralPath (PrivatePath $JobsFile)|ConvertFrom-Json
$app=New-Object -ComObject Word.Application
$security=$app.AutomationSecurity;$links=$app.Options.UpdateLinksAtOpen;$alerts=$app.DisplayAlerts;$visible=$app.Visible
$app.AutomationSecurity=3;$app.Options.UpdateLinksAtOpen=$false;$app.DisplayAlerts=0;$app.Visible=$false
try{
 foreach($job in $jobs){
  $source=PrivatePath $job.source;$pdf=PrivatePath $job.pdf;$json=PrivatePath $job.json
  if((Test-Path -LiteralPath $pdf)-or(Test-Path -LiteralPath $json)){throw 'Fresh outputs required'}
  $doc=$null
  try{
   $doc=$app.Documents.Open($source,$false,$true,$false);$fields=@()
   foreach($first in $doc.StoryRanges){$story=$first;while($null -ne $story){foreach($f in $story.Fields){
    $entry=@{story=[int]$story.StoryType;type=[int]$f.Type;code=[string]$f.Code.Text;before=[string]$f.Result.Text}
    if($job.update_fields -and [int]$f.Type -in @(22,3,26,33)){$entry.updated=[bool]$f.Update()}
    $entry.after=[string]$f.Result.Text;$fields+=$entry
   };$story=$story.NextStoryRange}}
   $doc.Repaginate();$paragraphs=@();$i=0
   foreach($p in $doc.Paragraphs){$i++;$start=$p.Range.Duplicate;$start.Collapse(1)
    $paragraphs+=@{index=$i;text=[string]$p.Range.Text;page=[int]$start.Information(3);y=[double]$start.Information(6);font=[string]$p.Range.Font.Name;fontSize=[double]$p.Range.Font.Size;spaceAfter=[double]$p.Format.SpaceAfter;lineSpacing=[double]$p.Format.LineSpacing;lineSpacingRule=[int]$p.Format.LineSpacingRule}
   }
   $doc.ExportAsFixedFormat($pdf,17)
   @{id=$job.id;wordVersion=$app.Version;wordBuild=$app.Build;pages=[int]$doc.ComputeStatistics(2);fields=$fields;paragraphs=$paragraphs}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath $json -Encoding utf8
   Write-Output ('Word read-only probe complete: '+$job.id)
  }finally{if($null -ne $doc){$doc.Close(0)}}
 }
}finally{$app.AutomationSecurity=$security;$app.Options.UpdateLinksAtOpen=$links;$app.DisplayAlerts=$alerts;$app.Visible=$visible;if($app.Documents.Count -eq 0){$app.Quit()};[void][Runtime.InteropServices.Marshal]::ReleaseComObject($app)}
