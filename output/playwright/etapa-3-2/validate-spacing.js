async (page) => {
  const baseUrl = 'http://localhost:3000/dev/design-system/spacing';
  const evidenceDir = 'D:/nite-connect/output/playwright/etapa-3-2';
  const widths = [320, 360, 768, 1024, 1280, 1440];
  const report = [];
  const failures = [];

  await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
  const themeControl = page.locator('select[aria-label="Tema da interface"]');
  await themeControl.waitFor({ state: 'visible' });
  await page.waitForFunction(() => {
    const control = document.querySelector('select[aria-label="Tema da interface"]');
    return control && !control.disabled;
  }, { timeout: 12000 });

  for (const theme of ['light', 'dark']) {
    await themeControl.selectOption(theme);
    await page.waitForFunction((expected) => document.documentElement.dataset.theme === expected, theme);

    for (const width of widths) {
      await page.setViewportSize({ width, height: 900 });
      await page.waitForTimeout(80);
      const checks = await page.evaluate(() => {
        const root = document.documentElement;
        const main = document.querySelector('main');
        const filterButton = document.querySelector('.spacing-lab-filter-action');
        const tableRegion = document.querySelector('.spacing-lab-table-scroll');
        const table = document.querySelector('.spacing-lab-table');
        const tableCell = document.querySelector('.spacing-lab-table td');
        const filterRange = document.createRange();
        filterRange.selectNodeContents(filterButton);
        const panel = document.querySelector('.spacing-lab-table-panel');
        const panelGrid = document.querySelector('.spacing-lab-panel-grid');
        const input = document.querySelector('#spacing-project-search');
        const style = getComputedStyle(main);
        const cellStyle = getComputedStyle(tableCell);
        const panelStyle = getComputedStyle(panel);
        return {
          viewport: innerWidth,
          theme: root.dataset.theme,
          backgroundToken: getComputedStyle(root).getPropertyValue('--nite-background').trim(),
          documentClientWidth: root.clientWidth,
          documentScrollWidth: root.scrollWidth,
          documentOverflowX: root.scrollWidth > root.clientWidth + 1,
          mainWidth: Math.round(main.getBoundingClientRect().width),
          mainMaxWidth: style.maxWidth,
          mainGutter: style.paddingLeft,
          panelColumns: getComputedStyle(panelGrid).gridTemplateColumns,
          filterButtonTextLines: filterRange.getClientRects().length,
          filterButtonWhiteSpace: getComputedStyle(filterButton).whiteSpace,
          tableRegionClientWidth: tableRegion.clientWidth,
          tableRegionScrollWidth: tableRegion.scrollWidth,
          tableRegionOwnsOverflow: tableRegion.scrollWidth > tableRegion.clientWidth + 1,
          inputHeight: Math.round(input.getBoundingClientRect().height),
          comfortableCellPadding: cellStyle.padding,
          comfortableCellFontSize: cellStyle.fontSize,
          comfortablePanelPadding: panelStyle.padding,
          selectedDensity: table.dataset.density,
        };
      });

      if (checks.documentOverflowX) failures.push({ theme, width, failure: 'overflow horizontal no documento', checks });
      if (checks.filterButtonTextLines !== 1 || checks.filterButtonWhiteSpace !== 'nowrap') failures.push({ theme, width, failure: 'texto do botão Filtros quebrou', checks });
      if (Number.parseFloat(checks.mainGutter) !== (width >= 1024 ? 32 : width >= 640 ? 24 : 16)) failures.push({ theme, width, failure: 'gutter não corresponde à faixa prevista', checks });
      if (width <= 360 && !checks.tableRegionOwnsOverflow) failures.push({ theme, width, failure: 'região tabular não contém rolagem própria', checks });
      report.push(checks);

      if (theme === 'dark' && width === 1280) {
        await page.screenshot({ path: `${evidenceDir}/spacing-dark-desktop.png`, fullPage: true });
      }
      if (theme === 'light' && width === 1280) {
        await page.screenshot({ path: `${evidenceDir}/spacing-light-desktop.png`, fullPage: true });
      }
      if (theme === 'dark' && width === 360) {
        await page.screenshot({ path: `${evidenceDir}/spacing-dark-mobile-360.png`, fullPage: true });
      }
      if (theme === 'light' && width === 360) {
        await page.screenshot({ path: `${evidenceDir}/spacing-light-mobile-360.png`, fullPage: true });
      }
    }

    if (theme === 'dark') {
      const comfortable = await page.evaluate(() => ({
        padding: getComputedStyle(document.querySelector('.spacing-lab-table td')).padding,
        fontSize: getComputedStyle(document.querySelector('.spacing-lab-table td')).fontSize,
        panelPadding: getComputedStyle(document.querySelector('.spacing-lab-table-panel')).padding,
      }));
      await page.getByRole('button', { name: 'Compacta' }).click();
      const compact = await page.evaluate(() => ({
        padding: getComputedStyle(document.querySelector('.spacing-lab-table td')).padding,
        fontSize: getComputedStyle(document.querySelector('.spacing-lab-table td')).fontSize,
        panelPadding: getComputedStyle(document.querySelector('.spacing-lab-table-panel')).padding,
        pressed: document.querySelector('button[aria-pressed="true"]')?.textContent?.trim(),
      }));
      if (comfortable.padding !== '12px 16px' || compact.padding !== '8px 12px') failures.push({ failure: 'padding das densidades divergente', comfortable, compact });
      if (comfortable.fontSize !== compact.fontSize) failures.push({ failure: 'densidade alterou tipografia', comfortable, compact });
      if (comfortable.panelPadding !== '24px' || compact.panelPadding !== '16px') failures.push({ failure: 'padding do painel de tabela divergente', comfortable, compact });
      if (compact.pressed !== 'Compacta') failures.push({ failure: 'botão de densidade não atualizou estado acessível', comfortable, compact });
      await page.screenshot({ path: `${evidenceDir}/spacing-density-compact-dark.png`, fullPage: true });
      report.push({ densityComparison: { comfortable, compact, typographyUnchanged: comfortable.fontSize === compact.fontSize } });
    }
  }

  await page.keyboard.press('Tab');
  const focus = await page.evaluate(() => ({
    activeTag: document.activeElement?.tagName,
    activeText: document.activeElement?.textContent?.trim(),
    outlineStyle: getComputedStyle(document.activeElement).outlineStyle,
    outlineWidth: getComputedStyle(document.activeElement).outlineWidth,
  }));
  if (focus.outlineStyle === 'none' || focus.outlineWidth === '0px') failures.push({ failure: 'foco visível não detectado', focus });

  const results = { url: baseUrl, viewportWidths: widths, themes: ['light', 'dark'], focus, report, failures };
  await page.evaluate((value) => { window.__spacingFoundationReport = value; }, results);
  return JSON.stringify(results, null, 2);
}
