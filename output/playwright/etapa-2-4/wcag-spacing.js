async (page) => {
  const baseUrl = 'http://localhost:3000/dev/design-system/typography';
  const evidenceDir = 'D:/nite-connect/output/playwright/etapa-2-4';
  const scenarios = [];
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

  for (const theme of ['light', 'dark']) {
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded' });
    const themeControl = page.locator('select[aria-label="Tema da interface"]');
    await themeControl.waitFor({ state: 'visible' });
    await page.waitForFunction(() => {
      const control = document.querySelector('select[aria-label="Tema da interface"]');
      return control && !control.disabled;
    }, { timeout: 12000 });
    await themeControl.selectOption(theme);
    await page.waitForFunction((expected) => document.documentElement.dataset.theme === expected, theme);
    const styleTag = await page.addStyleTag({ content: override });
    await page.locator('#type-project-name').fill('auditoria-wcag-1.4.12');
    await page.locator('#type-project-status').selectOption('progress');
    const otherTheme = theme === 'light' ? 'dark' : 'light';
    await themeControl.selectOption(otherTheme);
    const toggledTheme = await page.locator('html').getAttribute('data-theme');
    await themeControl.selectOption(theme);
    await page.waitForFunction((expected) => document.documentElement.dataset.theme === expected, theme);

    const capture = async (viewport) => page.evaluate(({ viewport, theme }) => {
      const px = (value) => Number.parseFloat(value) || 0;
      const visible = (element) => element.getClientRects().length > 0;
      const sampleSelectors = [
        ['display', '[data-type-token="display"] p'],
        ['pageTitle', '[data-type-token="page-title"] p'],
        ['metadata', '.typography-family-meta span'],
        ['label', 'label[for="type-project-name"]'],
        ['input', '#type-project-name'],
        ['tableHeader', '.typography-table-scroll th'],
        ['tableCell', '.typography-table-scroll td'],
        ['longText', '.typography-reflow-card p.text-type-body'],
        ['technicalId', '.typography-reflow-card p.text-type-technical'],
      ];
      const samples = sampleSelectors.map(([name, selector]) => {
        const element = document.querySelector(selector);
        if (!element) return { name, found: false };
        const style = getComputedStyle(element);
        const fontSize = px(style.fontSize);
        const lineHeight = px(style.lineHeight);
        return {
          name,
          found: true,
          fontSize,
          lineHeight,
          lineHeightRatio: Number((lineHeight / fontSize).toFixed(2)),
          letterSpacing: style.letterSpacing,
          wordSpacing: style.wordSpacing,
          width: Math.round(element.getBoundingClientRect().width),
          height: Math.round(element.getBoundingClientRect().height),
          scrollWidth: element.scrollWidth,
          clientWidth: element.clientWidth,
          text: (element.textContent || element.value || '').trim().slice(0, 80),
        };
      });
      const clipping = Array.from(document.querySelectorAll('body *')).filter((element) => {
        if (!visible(element)) return false;
        const style = getComputedStyle(element);
        const clipX = ['hidden', 'clip'].includes(style.overflowX) && element.scrollWidth > element.clientWidth + 1;
        const clipY = ['hidden', 'clip'].includes(style.overflowY) && element.scrollHeight > element.clientHeight + 1;
        return clipX || clipY;
      }).map((element) => ({
        tag: element.tagName,
        className: typeof element.className === 'string' ? element.className : '',
        text: (element.textContent || '').trim().slice(0, 60),
        overflowX: getComputedStyle(element).overflowX,
        overflowY: getComputedStyle(element).overflowY,
        scrollWidth: element.scrollWidth,
        clientWidth: element.clientWidth,
        scrollHeight: element.scrollHeight,
        clientHeight: element.clientHeight,
      }));
      const tableRegion = document.querySelector('.typography-table-scroll');
      const treeWalker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      const textRects = [];
      while (treeWalker.nextNode()) {
        const node = treeWalker.currentNode;
        if (!node.textContent.trim() || !visible(node.parentElement)) continue;
        if (node.parentElement.closest('.sr-only, nextjs-portal')) continue;
        const range = document.createRange();
        range.selectNodeContents(node);
        for (const rect of Array.from(range.getClientRects())) {
          if (rect.width > 1 && rect.height > 1) textRects.push({
            x: rect.x, y: rect.y, right: rect.right, bottom: rect.bottom,
            text: node.textContent.trim().slice(0, 35),
            parent: node.parentElement.tagName + '.' + (typeof node.parentElement.className === 'string' ? node.parentElement.className : ''),
          });
        }
      }
      const overlaps = [];
      for (let i = 0; i < textRects.length; i++) {
        for (let j = i + 1; j < textRects.length; j++) {
          const a = textRects[i], b = textRects[j];
          const iw = Math.min(a.right, b.right) - Math.max(a.x, b.x);
          const ih = Math.min(a.bottom, b.bottom) - Math.max(a.y, b.y);
          if (iw > 1.5 && ih > 1.5) overlaps.push({ a: a.text, aParent: a.parent, b: b.text, bParent: b.parent, intersection: [Math.round(iw), Math.round(ih)] });
        }
      }
      return {
        theme,
        viewport: { width: innerWidth, height: innerHeight, requested: viewport },
        document: { clientWidth: document.documentElement.clientWidth, scrollWidth: document.documentElement.scrollWidth, bodyScrollWidth: document.body.scrollWidth, pageHorizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1 },
        samples,
        clipping,
        overlapCount: overlaps.length,
        overlapExamples: overlaps.slice(0, 12),
        tableRegion: tableRegion ? { role: tableRegion.getAttribute('role'), tabIndex: tableRegion.tabIndex, width: tableRegion.clientWidth, scrollWidth: tableRegion.scrollWidth, overflowX: getComputedStyle(tableRegion).overflowX } : null,
        controls: {
          themeEnabled: !document.querySelector('select[aria-label="Tema da interface"]').disabled,
          formInputEnabled: !document.querySelector('#type-project-name').disabled,
          formSelectValue: document.querySelector('#type-project-status').value,
          formInputValue: document.querySelector('#type-project-name').value,
          foundationLinks: Array.from(document.querySelectorAll('.foundation-lab-navigation a')).map((a) => ({ text: a.textContent.trim(), href: a.getAttribute('href'), visible: visible(a) })),
        },
      };
    }, { viewport, theme });

    const desktop = await capture({ width: 1280, height: 900 });
    await page.screenshot({ path: `${evidenceDir}/wcag-spacing-${theme}-1280x900.png`, fullPage: true });
    await page.setViewportSize({ width: 320, height: 720 });
    const mobile = await capture({ width: 320, height: 720 });
    await page.screenshot({ path: `${evidenceDir}/wcag-spacing-${theme}-320x720.png`, fullPage: true });
    await styleTag.evaluate((style) => style.remove());
    scenarios.push({ theme, toggledTheme, desktop, mobile });
  }
  return JSON.stringify({ override: { lineHeight: '1.5', paragraphAfter: '2em', letterSpacing: '0.12em', wordSpacing: '0.16em' }, scenarios }, null, 2);
}
