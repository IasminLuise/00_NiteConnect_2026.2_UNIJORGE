package br.edu.unijorge.niteconnect.analytics.application;

import br.edu.unijorge.niteconnect.analytics.api.AnalyticsContracts.Report;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnalyticsExporter {
    public byte[] csv(Report report) {
        var lines=new ArrayList<String>();
        lines.add("Relatório NITE CONNECT");
        lines.add("Modelo,"+cell(report.type().name()));
        lines.add("Período,"+cell(report.from()+" a "+report.to()));
        lines.add("Critérios,"+cell(report.criteria()));
        lines.add("Gerado em,"+cell(report.generatedAt().toString()));
        lines.add(String.join(",",report.columns().stream().map(this::cell).toList()));
        for(var row:report.rows()) lines.add(String.join(",",row.stream().map(this::cell).toList()));
        lines.add("");
        lines.add("Métricas indisponíveis,"+cell(String.join("; ",report.unavailableMetrics())));
        return ("\uFEFF"+String.join("\r\n",lines)+"\r\n").getBytes(StandardCharsets.UTF_8);
    }

    public byte[] pdf(Report report) {
        try(PDDocument document=new PDDocument(); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            var regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            List<String> lines=new ArrayList<>();
            lines.add("NITE CONNECT | Relatório "+report.type().name());
            lines.add("Período: "+report.from()+" a "+report.to());
            lines.add("Gerado em: "+report.generatedAt());
            lines.add("Critérios: "+report.criteria());
            lines.add("Registros: "+report.total());
            lines.add("");
            for(var row:report.rows()) {
                for(int i=0;i<report.columns().size();i++) lines.add(report.columns().get(i)+": "+row.get(i));
                lines.add("---");
            }
            lines.add("Métricas indisponíveis:");
            report.unavailableMetrics().forEach(value->lines.add("- "+value));
            List<String> wrapped=wrap(lines,105);
            float y=PDRectangle.A4.getHeight()-48;
            PDPage page=new PDPage(PDRectangle.A4); document.addPage(page);
            PDPageContentStream stream=new PDPageContentStream(document,page);
            try {
                for(int i=0;i<wrapped.size();i++) {
                    String line=pdfText(wrapped.get(i));
                    if(y<42) { stream.close();page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);y=PDRectangle.A4.getHeight()-48; }
                    stream.beginText(); stream.setFont(i==0?bold:regular,i==0?12:9); stream.newLineAtOffset(40,y); stream.showText(line); stream.endText(); y-=14;
                }
            } finally { stream.close(); }
            document.save(output); return output.toByteArray();
        } catch(IOException ex) { throw new IllegalStateException("Não foi possível gerar o relatório PDF",ex); }
    }

    private String cell(String value) {
        String safe=value==null?"":value;
        if(safe.stripLeading().matches("^[=+@-].*")) safe="'"+safe;
        return "\""+safe.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";
    }
    private String pdfText(String value) {
        String cleaned=value.replaceAll("\\p{Cntrl}"," ");
        // Helvetica/WinAnsi handles Latin-1; replace unsupported glyphs so one bad source value cannot abort a report.
        return cleaned.codePoints().mapToObj(cp->cp<=255?new String(Character.toChars(cp)):"?").reduce("",String::concat);
    }
    private List<String> wrap(List<String> values,int width) {
        var result=new ArrayList<String>();
        for(String value:values) {
            String remaining=value;
            while(remaining.length()>width) {
                int split=remaining.lastIndexOf(' ',width);
                if(split<1) split=width;
                result.add(remaining.substring(0,split));
                remaining=remaining.substring(split).stripLeading();
            }
            result.add(remaining);
        }
        return result;
    }
}
