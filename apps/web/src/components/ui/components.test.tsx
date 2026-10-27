import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import {
  Badge,
  Button,
  Checkbox,
  Field,
  Select,
  Textarea,
  TextField,
} from "./index";

describe("componentes fundamentais da interface", () => {
  it("mantém Button como ação nativa e expõe variantes e estado carregando", () => {
    const html = renderToStaticMarkup(
      <>
        <Button type="submit" variant="secondary">Enviar</Button>
        <Button type="button" variant="quiet">Voltar</Button>
        <Button type="button" variant="destructive">Revogar acesso</Button>
        <Button type="button" loading>Salvando</Button>
      </>,
    );

    expect(html).toContain('<button type="submit"');
    expect(html).toContain('data-variant="secondary"');
    expect(html).toContain('data-variant="quiet"');
    expect(html).toContain('data-variant="destructive"');
    expect(html).toContain('aria-busy="true"');
    expect(html).toContain('disabled=""');
    expect(html).toContain("Salvando");
  });

  it("mantém TextField nativo, tipografia móvel e atributos de formulário", () => {
    const html = renderToStaticMarkup(
      <TextField
        type="email"
        name="email"
        required
        readOnly
        aria-invalid="true"
        aria-label="E-mail institucional"
      />,
    );

    expect(html).toContain('type="email"');
    expect(html).toContain('name="email"');
    expect(html).toContain('required=""');
    expect(html).toContain('readOnly=""');
    expect(html).toContain('aria-invalid="true"');
    expect(html).toContain("text-type-body");
    expect(html).toContain('data-slot="text-field"');
  });

  it("compartilha aparência e estados nativos com Textarea sem travar o tamanho", () => {
    const html = renderToStaticMarkup(
      <Textarea rows={4} disabled aria-label="Descrição" />,
    );

    expect(html).toContain("textarea");
    expect(html).toContain('rows="4"');
    expect(html).toContain('disabled=""');
    expect(html).toContain("text-type-body");
    expect(html).toContain("resize-y");
    expect(html).toContain('data-slot="textarea"');
    expect(html).not.toContain('height:');
  });

  it("associa Field ao controle e combina descrição, erro e referências existentes", () => {
    const html = renderToStaticMarkup(
      <Field
        id="project-title"
        label="Nome do projeto"
        description="Informe um nome curto."
        error="O nome precisa ser revisado."
        required
      >
        <TextField aria-describedby="external-help" />
      </Field>,
    );

    expect(html).toContain('for="project-title"');
    expect(html).toContain('id="project-title"');
    expect(html).toContain('aria-describedby="external-help project-title-description project-title-error"');
    expect(html).toContain('aria-invalid="true"');
    expect(html).toContain('required=""');
    expect(html).toContain('id="project-title-description"');
    expect(html).toContain('id="project-title-error"');
    expect(html).toContain("(obrigatório)");
  });

  it("identifica campos opcionais sem depender de cor", () => {
    const html = renderToStaticMarkup(
      <Field id="summary" label="Resumo" optional>
        <Textarea />
      </Field>,
    );

    expect(html).toContain("(opcional)");
    expect(html).toContain('for="summary"');
    expect(html).toContain('id="summary"');
  });

  it("mantém Select nativo, placeholder opcional e estados de formulário", () => {
    const html = renderToStaticMarkup(
      <>
        <Select name="team" required placeholder="Selecione uma equipe">
          <option value="research">Pesquisa</option>
        </Select>
        <Select name="project" defaultValue="portal" disabled>
          <option value="portal">Portal</option>
        </Select>
      </>,
    );

    expect(html).toContain('name="team" required=""');
    expect(html).toContain('<option value="" disabled="" selected="">Selecione uma equipe</option>');
    expect(html).toContain('data-slot="select"');
    expect(html).toContain('disabled=""');
    expect(html).toContain('value="portal" selected=""');
  });

  it("integra Select ao Field sem perder label, ajuda, erro e obrigatoriedade", () => {
    const html = renderToStaticMarkup(
      <Field
        id="project-kind"
        label="Categoria"
        description="Escolha uma opção."
        error="A seleção precisa ser revista."
        required
      >
        <Select placeholder="Selecione">
          <option value="extension">Extensão</option>
        </Select>
      </Field>,
    );

    expect(html).toContain('for="project-kind"');
    expect(html).toContain('id="project-kind"');
    expect(html).toContain('aria-describedby="project-kind-description project-kind-error"');
    expect(html).toContain('aria-invalid="true"');
    expect(html).toContain('required=""');
    expect(html).toContain('id="project-kind-error"');
  });

  it("preserva estados nativos de Checkbox e não serializa indeterminate como valor", () => {
    const html = renderToStaticMarkup(
      <>
        <Checkbox name="active" checked readOnly />
        <Checkbox name="pending" defaultChecked={false} indeterminate />
        <Checkbox name="disabled" disabled />
      </>,
    );

    expect(html).toContain('type="checkbox"');
    expect(html).toContain('data-slot="checkbox"');
    expect(html).toContain('name="active"');
    expect(html).toContain('checked=""');
    expect(html).toContain('name="pending"');
    expect(html).not.toContain('indeterminate=');
    expect(html).toContain('type="checkbox"');
    expect(html).toContain('disabled=""');
    expect(html).toContain('name="disabled"');
    expect(html.match(/type="checkbox"/g)).toHaveLength(3);
  });

  it("associa Checkbox a Field inline, incluindo descrição e erro", () => {
    const html = renderToStaticMarkup(
      <Field
        id="include-archived"
        label="Incluir itens arquivados"
        description="A opção pode ser alterada a qualquer momento."
        error="Confirme a preferência."
        layout="checkbox"
      >
        <Checkbox />
      </Field>,
    );

    expect(html).toContain('for="include-archived"');
    expect(html).toContain('id="include-archived"');
    expect(html).toContain('aria-describedby="include-archived-description include-archived-error"');
    expect(html).toContain('aria-invalid="true"');
    expect(html.indexOf('data-slot="checkbox"')).toBeLessThan(html.indexOf('for="include-archived"'));
  });

  it("renderiza Badge informativo, com variantes semânticas e sem papel interativo", () => {
    const html = renderToStaticMarkup(
      <>
        <Badge>Neutro</Badge>
        <Badge variant="institutional">Institucional</Badge>
        <Badge variant="success">Concluído</Badge>
        <Badge variant="warning">Atenção</Badge>
        <Badge variant="error">Revisar</Badge>
      </>,
    );

    expect(html).toContain('data-variant="neutral"');
    expect(html).toContain('data-variant="institutional"');
    expect(html).toContain("text-nite-brand-accent");
    expect(html).toContain('data-variant="success"');
    expect(html).toContain('data-variant="warning"');
    expect(html).toContain('data-variant="error"');
    expect(html).not.toContain('role="button"');
    expect(html).not.toContain('tabindex=');
    expect(html).toContain("Concluído");
  });
});
