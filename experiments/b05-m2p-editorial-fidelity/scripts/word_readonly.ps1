param(
    [Parameter(Mandatory=$true)][string]$JobsFile,
    [ValidateSet('ExportAsFixedFormat','SaveAs2')][string]$PdfMethod='ExportAsFixedFormat',
    [switch]$UpdateFields
)
# B05-M2P: Word Desktop is the reference; always read-only, never save source/candidate.
$ErrorActionPreference = 'Stop'
function AssertPrivate([string]$path) {
    $full=[IO.Path]::GetFullPath($path)
    $components=$full -split '[\\/]'
    if (-not ($components -contains '.private')) { throw 'All sources/results must remain beneath .private' }
    return $full
}
function GetFieldInventory($document) {
    $items=@();$stories=0
    foreach($first in $document.StoryRanges){
        $story=$first
        while($null -ne $story){
            $stories++
            if($stories -gt 128){throw 'Story traversal bound exceeded'}
            foreach($field in $story.Fields){$items+=@{story=[int]$story.StoryType;type=[int]$field.Type;code=[string]$field.Code.Text;result=[string]$field.Result.Text}}
            $story=$story.NextStoryRange
        }
    }
    return ,$items
}
function GetParagraphLayout($document) {
    $items=@();$index=0
    foreach($para in $document.Paragraphs){
        $point=$para.Range.Duplicate;$point.Collapse(1)
        $items+=[ordered]@{
            index=$index;page=[int]$point.Information(3);
            x=[double]$point.Information(5);y=[double]$point.Information(6);
            font=[string]$para.Range.Font.Name;fontSize=[double]$para.Range.Font.Size;
            after=[double]$para.Format.SpaceAfter;line=[double]$para.Format.LineSpacing;
            lineRule=[int]$para.Format.LineSpacingRule;text=[string]$para.Range.Text
        }
        $index++
    }
    return ,$items
}
$jobs=Get-Content -LiteralPath (AssertPrivate $JobsFile) -Raw -Encoding UTF8 | ConvertFrom-Json
if (@($jobs).Count -ne 2) { throw 'Exactly two independent source/candidate jobs required' }
$app=New-Object -ComObject Word.Application
$oldSecurity=$app.AutomationSecurity
$oldLinks=$app.Options.UpdateLinksAtOpen
$oldAlerts=$app.DisplayAlerts
$oldVisible=$app.Visible
$app.AutomationSecurity=3
$app.Options.UpdateLinksAtOpen=$false
$app.DisplayAlerts=0
$app.Visible=$false
try {
    foreach($job in @($jobs)) {
        if ([string]$job.id -notin @('source','candidate')) {throw 'Unexpected job id'}
        $inputFile=AssertPrivate ([string]$job.docx)
        $outPdf=AssertPrivate ([string]$job.pdf)
        $outJson=AssertPrivate ([string]$job.json)
        if (-not (Test-Path -LiteralPath $inputFile -PathType Leaf)) { throw 'DOCX not found' }
        if ((Test-Path -LiteralPath $outPdf) -or (Test-Path -LiteralPath $outJson)) {throw 'Fresh output paths required'}
        $doc=$null
        $hashBefore=(Get-FileHash -LiteralPath $inputFile -Algorithm SHA256).Hash
        try {
            # ConfirmConversions:false; ReadOnly:true; AddToRecentFiles:false; Revert:false
            $doc=$app.Documents.Open($inputFile,$false,$true,$false)
            if(-not $doc.ReadOnly){throw 'Word did not open read-only'}
            $doc.Repaginate()
            $fieldsBefore=GetFieldInventory $doc
            if($UpdateFields){
                $stories=0
                foreach($first in $doc.StoryRanges){
                    $story=$first
                    while($null -ne $story){
                        $stories++
                        if($stories -gt 128){throw 'Story traversal bound exceeded'}
                        [void]$story.Fields.Update()
                        $story=$story.NextStoryRange
                    }
                }
                $doc.Repaginate()
            }
            $fieldsAfterUpdate=GetFieldInventory $doc
            $layoutBefore=[ordered]@{pages=[int]$doc.ComputeStatistics(2);paragraphs=(GetParagraphLayout $doc)}
            if($PdfMethod -eq 'SaveAs2'){
                # Explicit diagnostic alternative when ExportAsFixedFormat stalls.
                # This writes PDF only; the original DOCX must remain byte-identical.
                [object]$pdfName=[string]$outPdf
                [object]$pdfFormat=17
                $doc.SaveAs2([ref]$pdfName,[ref]$pdfFormat)
            } else {$doc.ExportAsFixedFormat($outPdf,17)}
            $doc.Repaginate()
            $paragraphs=GetParagraphLayout $doc
            $pages=[int]$doc.ComputeStatistics(2)
            $layoutAfter=[ordered]@{pages=$pages;paragraphs=$paragraphs}
            $layoutChanged=(($layoutBefore | ConvertTo-Json -Depth 9 -Compress) -cne ($layoutAfter | ConvertTo-Json -Depth 9 -Compress))
            $fields=GetFieldInventory $doc
            $doc.Close(0);$doc=$null
            $hashAfter=(Get-FileHash -LiteralPath $inputFile -Algorithm SHA256).Hash
            if($hashBefore -ne $hashAfter){throw 'Source bytes changed'}
            @{id=[string]$job.id;wordVersion=$app.Version;wordBuild=$app.Build;pages=$pages;
                paragraphs=$paragraphs;fields=$fields;readOnly=$true;sourceUnmodified=($hashBefore -eq $hashAfter);
                sha256Before=$hashBefore;sha256After=$hashAfter;pdfMethod=$PdfMethod;
                layoutBeforePdf=$layoutBefore;layoutChangedDuringPdf=$layoutChanged;
                fieldsExplicitlyUpdated=[bool]$UpdateFields;fieldsBeforeUpdate=$fieldsBefore;
                fieldsAfterUpdate=$fieldsAfterUpdate} |
                ConvertTo-Json -Depth 9 | Set-Content -LiteralPath $outJson -Encoding UTF8
            Write-Output ('Readonly Word evaluation finished: '+$job.id)
        } finally {if($null -ne $doc){$doc.Close(0)}}
    }
} finally {
    $app.AutomationSecurity=$oldSecurity
    $app.Options.UpdateLinksAtOpen=$oldLinks
    $app.DisplayAlerts=$oldAlerts
    $app.Visible=$oldVisible
    if($app.Documents.Count -eq 0){$app.Quit()}
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($app)
}
