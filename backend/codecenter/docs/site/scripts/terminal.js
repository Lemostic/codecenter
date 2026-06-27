/* ============================================================
 * terminal.js — 模拟终端打字机
 * 进入视区后逐行打字，支持命令/输出/注释三种样式与光标
 * ============================================================ */

(function () {
  /**
   * @param {HTMLElement} body  终端输出容器
   * @param {Array<{text:string,type:'cmd'|'out'|'note'|'ok'|'warn',pause?:number}>} lines
   * @param {() => void} [onDone]
   */
  function playTerminal(body, lines, onDone) {
    body.innerHTML = '';
    let i = 0;
    const SPEED = 16;          // 单字符 ms
    const LINE_GAP = 260;      // 行间 ms
    const cursor = makeCursor();

    function nextLine() {
      if (i >= lines.length) {
        cursor.remove();
        if (onDone) onDone();
        return;
      }
      const ln = lines[i++];
      const row = document.createElement('div');
      row.className = 'term-line t-' + (ln.type || 'out');
      body.appendChild(row);
      body.appendChild(cursor);
      typeRow(row, ln.text, ln.type, () => {
        setTimeout(nextLine, ln.pause != null ? ln.pause : LINE_GAP);
      });
    }

    function typeRow(row, text, type, done) {
      // 注释/输出类直接整行闪现，命令类逐字打
      if (type === 'out' || type === 'ok' || type === 'warn' || type === 'note') {
        row.textContent = text;
        body.scrollTop = body.scrollHeight;
        setTimeout(done, 120);
        return;
      }
      let idx = 0;
      (function tick() {
        row.textContent = '$ ' + text.slice(0, idx);
        body.scrollTop = body.scrollHeight;
        idx++;
        if (idx <= text.length) {
          setTimeout(tick, SPEED);
        } else {
          done();
        }
      })();
    }

    nextLine();
  }

  function makeCursor() {
    const c = document.createElement('span');
    c.className = 'term-cursor';
    c.textContent = '▋';
    return c;
  }

  window.SPEC_TERMINAL = { playTerminal };
})();
