"""Source-free input with a real last-save property for SAVEDATE controls."""
import io
import zipfile
from compatibility_cases import field_package


def savedate_source():
    original = field_package()
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(original)) as source, zipfile.ZipFile(output, 'w') as target:
        for entry in source.infolist():
            blob = source.read(entry)
            if entry.filename == '[Content_Types].xml':
                blob = blob.replace(b'</Types>', b'<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>')
            if entry.filename == '_rels/.rels':
                blob = blob.replace(b'</Relationships>', b'<Relationship Id="rIdCore" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/></Relationships>')
            target.writestr(entry, blob)
        target.writestr('docProps/core.xml', b'<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dcterms:created xsi:type="dcterms:W3CDTF">2026-09-23T23:56:00Z</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">2026-09-23T23:56:00Z</dcterms:modified></cp:coreProperties>')
    return output.getvalue()
