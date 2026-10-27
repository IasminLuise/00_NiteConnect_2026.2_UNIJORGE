async (page) => {
  const results = [];
  const failures = [];
  const override = `
    html body * {
      line-height: 1.5 !important;
      letter-spacing: 0.12em !important;
      word-spacing: 0.16em !important;
    }
    html body p {
      margin-block-start: 0 !important;
      margin-block-end: 2em !important;
    }
  `;

  await page.goto('http://localhost:3000/dev/design-system/spacing', { waitUntil: 'domcontentloaded' });
  await page.waitForFunction(() => {
    const control = document.querySelector('select[aria-label="Tema da interface"]');
    return control && !control.disabled;
  }, { timeout: 12000 });
  const styleTag = await page.addStyleTag({ content: override });

  for (const theme of ['light', 'dark']) {
    await page.locator('select[aria-label="Tema da interface"]').selectOption(theme);
    await page.waitForFunction((expected) => document.documentElement.dataset.theme === expected, theme);
    for (const width of [320, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page.waitForTimeout(80);
      await page.locator('#spacing-project-search').fill('Consulta longa para conferir reflow');
      await page.locator('#spacing-project-status').selectOption('progress');
      const check = await page.evaluate(() => {
        const root = document.documentElement;
        const filterButton = document.querySelector('.spacing-lab-filter-action');
        const range = document.createRange();
        range.selectNodeContents(filterButton);
        const field = document.querySelector('#spacing-project-search');
        const longCopy = document.querySelector('.spacing-lab-long-copy');
        const tableRegion = document.querySelector('.spacing-lab-table-scroll');
        return {
          theme: root.dataset.theme,
          viewport: innerWidth,
          pageOverflowX: root.scrollWidth > root.clientWidth + 1,
          filterTextLines: range.getClientRects().length,
          inputValue: field.value,
          selectedFilter: document.querySelector('#spacing-project-status').value,
          inputVisible: field.getClientRects().length > 0,
          tableOwnsHorizontalOverflow: tableRegion.scrollWidth > tableRegion.clientWidth + 1,
          longCopyHeight: Math.round(longCopy.getBoundingClientRect().height),
          longCopyScrollHeight: longCopy.scrollHeight,
          longCopyClientHeight: longCopy.clientHeight,
          clippedLongCopy: longCopy.scrollHeight > longCopy.clientHeight + 1,
        };
      });
      if (check.pageOverflowX) failures.push({ ...check, failure: 'overflow horizontal da página com espaçamento textual ampliado' });
      if (check.filterTextLines !== 1) failures.push({ ...check, failure: 'ação de filtro quebrou em múltiplas linhas' });
      if (!check.inputVisible || !check.inputValue || check.selectedFilter !== 'progress') failures.push({ ...check, failure: 'campo ou seleção perdeu interação' });
      if (check.clippedLongCopy) failures.push({ ...check, failure: 'texto longo foi cortado' });
      results.push(check);
    }
  }

  await styleTag.evaluate((style) => style.remove());
  const report = { override: { lineHeight: '1.5', paragraphSpacing: '2em', letterSpacing: '0.12em', wordSpacing: '0.16em' }, results, failures };
  await page.evaluate((value) => { window.__spacingTextReport = value; }, report);
  return JSON.stringify(report, null, 2);
}
