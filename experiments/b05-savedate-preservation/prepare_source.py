"""Apply the native SAVEDATE source change to a clean, pinned private SDK checkout."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

REVISION='3679e0375a758d7dc59d31337a45461be3525ed8'


def prepare(root: Path, source_archive: Path | None = None):
    root=root.resolve(strict=True)
    if '.private' not in root.parts: raise ValueError('Private SDK checkout required')
    lock=json.loads(Path(__file__).with_name('source.lock.json').read_text(encoding='utf-8'))
    if source_archive is not None:
        if hashlib.sha256(source_archive.read_bytes()).hexdigest()!=lock['source_archive_sha256']:
            raise ValueError('Unsupported source archive')
        sha=lock['revision']
    else:
        sha=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
        if sha!=REVISION: raise ValueError('Unsupported source revision')
        if subprocess.check_output(['git','status','--porcelain'],cwd=root): raise ValueError('Clean source checkout required')
    for file,expected in lock['original_files'].items():
        # Git checkout line endings may differ; source identity is canonical LF.
        actual=(root/file).read_text(encoding='utf-8').encode()
        if hashlib.sha256(actual).hexdigest()!=expected: raise ValueError('Modified source input: '+file)
    edits={}
    def replace(file,before,after):
        value=edits.get(file,(root/file).read_text(encoding='utf-8'))
        if value.count(before)!=1: raise ValueError('Unexpected source anchor: '+file)
        edits[file]=value.replace(before,after,1)
    parser='word/Editor/Paragraph/ComplexFieldInstruction.js'
    replace(parser,'\telse if ("DATE" === sBuffer)', '\telse if ("SAVEDATE" === sBuffer)\n\t{\n\t\tthis.private_ReadSAVEDATE();\n\t}\n\telse if ("DATE" === sBuffer)')
    field='word/Editor/Field.js'
    replace(field,'\tif (this.FieldType === AscWord.fieldtype_SEQ)\n\t{', '''\tif (this.FieldType === AscWord.fieldtype_SAVEDATE)
\t{
\t\tlet result = AscWord.calculateSavedate(this.GetInstruction(), this.Paragraph.GetLogicDocument(), this.GetFirstRunNonEmpty());
\t\tif (!result.ok) return;
\t\tsReplaceString = result.value;
\t}
\telse if (this.FieldType === AscWord.fieldtype_SEQ)
\t{''')
    replace(field,'\t\t\tres.AddToContentToEnd(new AscWord.CRunPagesCount(isNaN(pageCount) ? undefined : pageCount));', '''\t\t\tlet pageCountField = new AscWord.CRunPagesCount(isNaN(pageCount) ? undefined : pageCount);
\t\t\tpageCountField.fieldInstruction = this.GetInstructionLine();
\t\t\tres.AddToContentToEnd(pageCountField);''')
    replace(field,'\t\tcase AscWord.fieldtype_DATE :', '\t\tcase AscWord.fieldtype_SAVEDATE :\n\t\t{\n\t\t\tname = "SAVEDATE";\n\t\t\tbreak;\n\t\t}\n\t\tcase AscWord.fieldtype_DATE :')
    complex='word/Editor/Paragraph/ComplexField.js'
    replace(complex,'\tthis.SelectFieldValue();\n\n\tif (true === isCreateHistoryPoint)', '''\tif (this.Instruction.GetType() === AscWord.fieldtype_SAVEDATE
\t\t&& !AscWord.calculateSavedate(this.Instruction, this.LogicDocument, this.private_GetSavedateRun()).ok)
\t\treturn false;
\tthis.SelectFieldValue();

\tif (true === isCreateHistoryPoint)''')
    replace(complex,'\t\tcase AscWord.fieldtype_TIME:\n\t\tcase AscWord.fieldtype_DATE:\n\t\t\tthis.private_UpdateTIME();', '\t\tcase AscWord.fieldtype_SAVEDATE:\n\t\t\tthis.private_UpdateSAVEDATE();\n\t\t\tbreak;\n\t\tcase AscWord.fieldtype_TIME:\n\t\tcase AscWord.fieldtype_DATE:\n\t\t\tthis.private_UpdateTIME();')
    replace(complex,'\t\tcase AscWord.fieldtype_TIME:\n\t\tcase AscWord.fieldtype_DATE:\n\t\t\tsResult = this.private_CalculateTIME();', '\t\tcase AscWord.fieldtype_SAVEDATE:\n\t\t\tvar savedate = AscWord.calculateSavedate(this.Instruction, this.LogicDocument, this.private_GetSavedateRun());\n\t\t\tsResult = savedate.ok ? savedate.value : this.GetFieldValueText();\n\t\t\tbreak;\n\t\tcase AscWord.fieldtype_TIME:\n\t\tcase AscWord.fieldtype_DATE:\n\t\t\tsResult = this.private_CalculateTIME();')
    serialize='word/Editor/Serialize2.js'
    replace(serialize,'\t\t\t\t\tInstr = "NUMPAGES \\\\* MERGEFORMAT";', '\t\t\t\t\tInstr = elem.pageNum.fieldInstruction || "NUMPAGES \\\\* MERGEFORMAT";')
    pages='word/Editor/Paragraph/RunContent/PagesCount.js'
    replace(pages,'\tCRunPagesCount.prototype.UpdatePageCount = function(pageCount)\n\t{', '''\tCRunPagesCount.prototype.Copy = function()
\t{
\t\tvar copy = new CRunPagesCount(this.pageNum);
\t\tcopy.numFormat = this.numFormat;
\t\tcopy.numText = this.numText;
\t\tcopy.textPr = this.textPr;
\t\tcopy.graphemes = this.graphemes.slice();
\t\tcopy.widths = this.widths.slice();
\t\tcopy.Width = this.Width;
\t\tcopy.WidthVisible = this.WidthVisible;
\t\tcopy.fieldInstruction = this.fieldInstruction;
\t\treturn copy;
\t};
\tCRunPagesCount.prototype.UpdatePageCount = function(pageCount)
\t{''')
    replace(complex,'CComplexField.prototype.private_UpdateTIME = function(ms)', '''CComplexField.prototype.private_GetSavedateRun = function()
{
\tlet codeRun = AscWord.getSavedateInstructionRun(this);
\tif (codeRun) return codeRun;
\tlet separate = this.GetSeparateChar();
\treturn separate ? separate.GetRun() : null;
};
CComplexField.prototype.private_InsertSavedateSegments = function(segments)
{
\tvar selectedContent = new AscCommonWord.CSelectedContent();
\tvar para = new AscWord.Paragraph();
\tfor (var i = 0; i < segments.length; i++)
\t{
\t\tif (!segments[i].text)
\t\t\tcontinue;
\t\tvar run = new ParaRun(para, false);
\t\tif (segments[i].textPr)
\t\t{
\t\t\tvar textPr = segments[i].textPr.Copy ? segments[i].textPr.Copy() : segments[i].textPr;
\t\t\trun.Apply_Pr(textPr);
\t\t}
\t\trun.AddText(segments[i].text);
\t\tpara.AddToContentToEnd(run);
\t}
\tselectedContent.Add(new AscCommonWord.CSelectedElement(para, false));
\tthis.private_InsertContent(selectedContent);
};
CComplexField.prototype.private_UpdateSAVEDATE = function()
{
\tlet codeRun = this.private_GetSavedateRun();
\tlet result = AscWord.calculateSavedate(this.Instruction, this.LogicDocument, codeRun);
\tif (!result.ok) return;
\tlet textPr = this.GetFieldValueTextPr();
\tlet codeTextPr = codeRun && codeRun.GetDirectTextPr ? codeRun.GetDirectTextPr() : null;
\tlet segments = this.Instruction.CharFormat && codeTextPr
\t\t? [{text: result.value, textPr: codeTextPr}]
\t\t: AscWord.savedateFormatSegments(result.value, this.Instruction, textPr, codeTextPr);
\tthis.private_InsertSavedateSegments(segments);
};
CComplexField.prototype.private_UpdateTIME = function(ms)''')
    config='configs/word.json'
    replace(config,'"word/Editor/Paragraph/ComplexFieldInstruction.js",','"word/Editor/Paragraph/ComplexFieldInstruction.js",\n\t\t\t"word/Editor/Paragraph/ComplexFields/savedate.js",')
    new='word/Editor/Paragraph/ComplexFields/savedate.js'
    edits[new]=Path(__file__).with_name('native').joinpath('savedate.js').read_text(encoding='utf-8')
    # Validate every input/anchor before writing any vendor source.
    for file,value in edits.items():
        with (root/file).open('w',encoding='utf-8',newline='\n') as stream: stream.write(value)
    return {'upstream_revision':sha,'files':{f:hashlib.sha256(v.encode()).hexdigest() for f,v in edits.items()}}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--sdk-source',required=True,type=Path)
    parser.add_argument('--source-archive',type=Path)
    args=parser.parse_args();print(json.dumps(prepare(args.sdk_source,args.source_archive),indent=2))
