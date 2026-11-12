/*
 * admin.js — 관리 화면 (R5·R6·R6-2·R8-3)
 *
 * 사용하는 API:
 *   GET  /api/members                     → [{id, name, role, turnOrder, active, mustChangePw}]
 *   POST /api/members                     ← {name, password}
 *   PUT  /api/members/{id}                ← {name}
 *   DELETE /api/members/{id}
 *   PUT  /api/members/order               ← {ids: [순서대로 id]}
 *   PUT  /api/members/{id}/password       ← {password}
 *   GET  /api/zones                       → [{id, name, zoneOrder, active}]
 *   POST /api/zones                       ← {name}
 *   PUT  /api/zones/{id}                  ← {name}
 *   DELETE /api/zones/{id}
 *   PUT  /api/zones/order                 ← {ids: [순서대로 id]}
 *   GET  /api/history?weekStart=YYYY-MM-DD → {weeks:[{weekStart, duties:[{zoneId,zoneName,assigneeId,assigneeName,status,doneById,doneByName,doneDate}]}]}
 *   PUT  /api/duty/last-week              ← {zoneId, status, weekStart, doneById?, doneDate?}
 *   POST /api/rooms/close
 */

// ── 상태 ─────────────────────────────────────────────────────
let members    = [];   // 활성 멤버 순서 배열
let allMembers = [];   // 전체 멤버 (비활성 포함)
let zones      = [];   // 활성 구역 순서 배열
let allZones   = [];   // 전체 구역 (비활성 포함)
let lastWeekDuties = [];
let lastWeekDate   = '';   // "YYYY-MM-DD"

// ── 초기화 ───────────────────────────────────────────────────
$(function () {
    Api.me().then(function (me) {
        Page.renderHeader(me);
        if (!me || me.role !== 'ADMIN') { location.href = 'index.html'; return; }
        if (Page.guardMustChangePw(me)) return;
        loadAll();
    }).catch(function () {
        location.href = 'login.html';
    });

    // ── 멤버 이벤트 ─────────────────────────────────────────
    $(document).on('click', '#add-member-btn', addMember);

    $(document).on('click', '.move-member-up', function () {
        moveMember(parseInt($(this).data('id'), 10), -1);
    });
    $(document).on('click', '.move-member-down', function () {
        moveMember(parseInt($(this).data('id'), 10), 1);
    });
    $(document).on('click', '.rename-member-btn', function () {
        renameMember(parseInt($(this).data('id'), 10));
    });
    $(document).on('click', '.reset-pw-btn', function () {
        const id = parseInt($(this).data('id'), 10);
        $('#pw-form-' + id).toggleClass('open');
    });
    $(document).on('click', '.confirm-pw-btn', function () {
        const id = parseInt($(this).data('id'), 10);
        const pw = $('#pw-input-' + id).val();
        if (!pw) { Page.showMessage('비밀번호를 입력하세요.'); return; }
        Page.clearMessage();
        Api.put('/api/members/' + id + '/password', { password: pw }).then(function () {
            Page.showMessage('초기화했습니다. 다음 로그인 때 바꿔야 합니다.', 'info');
            $('#pw-form-' + id).removeClass('open');
            $('#pw-input-' + id).val('');
        }).catch(function (err) { Page.showMessage(err.message); });
    });
    $(document).on('click', '.delete-member-btn', function () {
        deleteMember(parseInt($(this).data('id'), 10));
    });

    // ── 구역 이벤트 ─────────────────────────────────────────
    $(document).on('click', '#add-zone-btn', addZone);

    $(document).on('click', '.move-zone-up', function () {
        moveZone(parseInt($(this).data('id'), 10), -1);
    });
    $(document).on('click', '.move-zone-down', function () {
        moveZone(parseInt($(this).data('id'), 10), 1);
    });
    $(document).on('click', '.rename-zone-btn', function () {
        renameZone(parseInt($(this).data('id'), 10));
    });
    $(document).on('click', '.delete-zone-btn', function () {
        deleteZone(parseInt($(this).data('id'), 10));
    });

    // ── 보정 이벤트 ─────────────────────────────────────────
    $(document).on('click', '.correct-done-btn', function () {
        const zoneId = $(this).data('zone-id');
        $('#done-form-' + zoneId).toggleClass('open');
    });
    $(document).on('click', '.confirm-done-btn', function () {
        const zoneId = $(this).data('zone-id');
        const doneById = parseInt($('#done-member-' + zoneId).val(), 10);
        const doneDate = $('#done-date-' + zoneId).val();
        if (!doneById || !doneDate) { Page.showMessage('청소한 사람과 날짜를 선택하세요.'); return; }
        Page.clearMessage();
        Api.put('/api/duty/last-week', {
            zoneId: zoneId,
            status: 'DONE',
            weekStart: lastWeekDate,
            doneById: doneById,
            doneDate: doneDate
        }).then(function () {
            Page.showMessage('보정되었습니다.', 'info');
            $('#done-form-' + zoneId).removeClass('open');
            loadLastWeek();
        }).catch(function (err) { Page.showMessage(err.message); });
    });
    $(document).on('click', '.correct-missed-btn', function () {
        const zoneId = $(this).data('zone-id');
        if (!confirm('이 구역을 미완료로 보정하시겠습니까?')) return;
        Page.clearMessage();
        Api.put('/api/duty/last-week', {
            zoneId: zoneId,
            status: 'MISSED',
            weekStart: lastWeekDate
        }).then(function () {
            Page.showMessage('보정되었습니다.', 'info');
            loadLastWeek();
        }).catch(function (err) { Page.showMessage(err.message); });
    });

    // ── 방 종료 ─────────────────────────────────────────────
    $(document).on('click', '#close-room-btn', closeRoom);
});

function loadAll() {
    loadMembers();
    loadZones();
    loadLastWeek();
}

// ── 룸메이트 ─────────────────────────────────────────────────

function loadMembers() {
    Api.get('/api/members').then(function (data) {
        allMembers = data || [];
        members = allMembers.filter(function (m) { return m.active; });
        renderMembers();
    }).catch(function (err) { Page.showMessage(err.message); });
}

function renderMembers() {
    const tbody = $('#member-tbody');
    tbody.empty();
    allMembers.forEach(function (m) {
        const isInactive = !m.active;
        const rowClass = isInactive ? 'inactive-row' : '';
        const activeIdx = members.findIndex(function (am) { return am.id === m.id; });

        let upBtn = '', downBtn = '';
        if (!isInactive) {
            const disUp  = activeIdx <= 0 ? 'disabled' : '';
            const disDown = activeIdx >= members.length - 1 ? 'disabled' : '';
            upBtn   = '<button class="order-btn move-member-up" data-id="' + m.id + '" ' + disUp   + '>▲</button>';
            downBtn = '<button class="order-btn move-member-down" data-id="' + m.id + '" ' + disDown + '>▼</button>';
        }

        let actions = '';
        if (!isInactive) {
            actions += '<button class="btn-sm rename-member-btn" data-id="' + m.id + '">이름</button> ';
            actions += '<button class="btn-sm reset-pw-btn" data-id="' + m.id + '">비밀번호</button> ';
            actions += '<button class="btn-sm danger delete-member-btn" data-id="' + m.id + '">삭제</button>';
            actions += '<div class="inline-form" id="pw-form-' + m.id + '">'
                + '<input type="password" id="pw-input-' + m.id + '" placeholder="새 비밀번호">'
                + '<button class="btn-sm primary confirm-pw-btn" data-id="' + m.id + '">저장</button>'
                + '</div>';
        }

        const statusText = isInactive ? '비활성' : (m.mustChangePw ? '비밀번호 변경 필요' : '활성');
        const roleText   = m.role === 'ADMIN' ? '관리자' : '멤버';

        tbody.append(
            '<tr class="' + rowClass + '">'
            + '<td class="order-btns">' + upBtn + downBtn + '</td>'
            + '<td>' + Page.esc(m.name) + '</td>'
            + '<td>' + roleText + '</td>'
            + '<td>' + statusText + '</td>'
            + '<td class="actions-col">' + actions + '</td>'
            + '</tr>'
        );
    });
}

function moveMember(id, direction) {
    const idx = members.findIndex(function (m) { return m.id === id; });
    if (idx < 0) return;
    const newIdx = idx + direction;
    if (newIdx < 0 || newIdx >= members.length) return;
    const tmp = members[idx];
    members[idx] = members[newIdx];
    members[newIdx] = tmp;
    Api.put('/api/members/order', { ids: members.map(function (m) { return m.id; }) })
        .then(loadMembers)
        .catch(function (err) { Page.showMessage(err.message); loadMembers(); });
}

function addMember() {
    const name = $('#new-member-name').val().trim();
    const pw   = $('#new-member-pw').val();
    if (!name) { Page.showMessage('이름을 입력하세요.'); return; }
    if (!pw)   { Page.showMessage('비밀번호를 입력하세요.'); return; }
    Page.clearMessage();
    Api.post('/api/members', { name: name, password: pw }).then(function () {
        $('#new-member-name').val('');
        $('#new-member-pw').val('');
        loadMembers();
    }).catch(function (err) { Page.showMessage(err.message); });
}

function renameMember(id) {
    const member = allMembers.find(function (m) { return m.id === id; });
    const newName = prompt('새 이름을 입력하세요.', member ? member.name : '');
    if (newName === null) return;
    const trimmed = newName.trim();
    if (!trimmed) { Page.showMessage('이름을 입력하세요.'); return; }
    Page.clearMessage();
    Api.put('/api/members/' + id, { name: trimmed })
        .then(loadMembers)
        .catch(function (err) { Page.showMessage(err.message); });
}

function deleteMember(id) {
    const member = allMembers.find(function (m) { return m.id === id; });
    const name   = member ? member.name : '이 멤버';
    if (!confirm(name + '을(를) 삭제하시겠습니까?\n지난 기록은 유지되고 다음 주부터 빠집니다.')) return;
    Page.clearMessage();
    Api.del('/api/members/' + id)
        .then(loadMembers)
        .catch(function (err) { Page.showMessage(err.message); });
}

// ── 구역 ─────────────────────────────────────────────────────

function loadZones() {
    Api.get('/api/zones').then(function (data) {
        allZones = data || [];
        zones = allZones.filter(function (z) { return z.active; });
        renderZones();
    }).catch(function (err) { Page.showMessage(err.message); });
}

function renderZones() {
    const tbody = $('#zone-tbody');
    tbody.empty();
    allZones.forEach(function (z) {
        const isInactive = !z.active;
        const rowClass   = isInactive ? 'inactive-row' : '';
        const activeIdx  = zones.findIndex(function (az) { return az.id === z.id; });

        let upBtn = '', downBtn = '';
        if (!isInactive) {
            const disUp   = activeIdx <= 0 ? 'disabled' : '';
            const disDown = activeIdx >= zones.length - 1 ? 'disabled' : '';
            upBtn   = '<button class="order-btn move-zone-up" data-id="' + z.id + '" ' + disUp   + '>▲</button>';
            downBtn = '<button class="order-btn move-zone-down" data-id="' + z.id + '" ' + disDown + '>▼</button>';
        }

        let actions = '';
        if (!isInactive) {
            actions += '<button class="btn-sm rename-zone-btn" data-id="' + z.id + '">이름</button> ';
            actions += '<button class="btn-sm danger delete-zone-btn" data-id="' + z.id + '">삭제</button>';
        }

        tbody.append(
            '<tr class="' + rowClass + '">'
            + '<td class="order-btns">' + upBtn + downBtn + '</td>'
            + '<td>' + Page.esc(z.name) + '</td>'
            + '<td>' + (isInactive ? '비활성' : '활성') + '</td>'
            + '<td class="actions-col">' + actions + '</td>'
            + '</tr>'
        );
    });
}

function moveZone(id, direction) {
    const idx = zones.findIndex(function (z) { return z.id === id; });
    if (idx < 0) return;
    const newIdx = idx + direction;
    if (newIdx < 0 || newIdx >= zones.length) return;
    const tmp = zones[idx];
    zones[idx] = zones[newIdx];
    zones[newIdx] = tmp;
    Api.put('/api/zones/order', { ids: zones.map(function (z) { return z.id; }) })
        .then(loadZones)
        .catch(function (err) { Page.showMessage(err.message); loadZones(); });
}

function addZone() {
    const name = $('#new-zone-name').val().trim();
    if (!name) { Page.showMessage('구역 이름을 입력하세요.'); return; }
    Page.clearMessage();
    Api.post('/api/zones', { name: name }).then(function () {
        $('#new-zone-name').val('');
        loadZones();
    }).catch(function (err) { Page.showMessage(err.message); });
}

function renameZone(id) {
    const zone    = allZones.find(function (z) { return z.id === id; });
    const newName = prompt('새 구역 이름을 입력하세요.', zone ? zone.name : '');
    if (newName === null) return;
    const trimmed = newName.trim();
    if (!trimmed) { Page.showMessage('이름을 입력하세요.'); return; }
    Page.clearMessage();
    Api.put('/api/zones/' + id, { name: trimmed })
        .then(loadZones)
        .catch(function (err) { Page.showMessage(err.message); });
}

function deleteZone(id) {
    const zone = allZones.find(function (z) { return z.id === id; });
    const name = zone ? zone.name : '이 구역';
    if (!confirm(name + '을(를) 삭제하시겠습니까?\n지난 기록은 유지되고 다음 주부터 빠집니다.')) return;
    Page.clearMessage();
    Api.del('/api/zones/' + id)
        .then(loadZones)
        .catch(function (err) { Page.showMessage(err.message); });
}

// ── 지난 주 보정 ──────────────────────────────────────────────

function getLastMonday() {
    const today      = new Date();
    const dayOfWeek  = today.getDay();
    const toMonday   = (dayOfWeek + 6) % 7;
    const lastMon    = new Date(today);
    lastMon.setDate(today.getDate() - toMonday - 7);
    const y = lastMon.getFullYear();
    const m = String(lastMon.getMonth() + 1).padStart(2, '0');
    const d = String(lastMon.getDate()).padStart(2, '0');
    return y + '-' + m + '-' + d;
}

function getLastSunday(lastMonday) {
    const d = new Date(lastMonday);
    d.setDate(d.getDate() + 6);
    return d.toISOString().slice(0, 10);
}

function loadLastWeek() {
    lastWeekDate = getLastMonday();
    $('#last-week-label').text('지난 주: ' + lastWeekDate + ' 주');
    Api.get('/api/history?weekStart=' + lastWeekDate).then(function (data) {
        lastWeekDuties = (data && data.weeks && data.weeks[0]) ? data.weeks[0].duties : [];
        renderLastWeek();
    }).catch(function (err) {
        if (err.status === 404) {
            $('#last-week-list').html('<p class="hint">지난 주 기록이 없습니다.</p>');
        } else {
            Page.showMessage(err.message);
        }
    });
}

function renderLastWeek() {
    const container  = $('#last-week-list');
    const lastSunday = getLastSunday(lastWeekDate);
    container.empty();
    if (!lastWeekDuties || lastWeekDuties.length === 0) {
        container.html('<p class="hint">지난 주 기록이 없습니다.</p>');
        return;
    }

    // 보정 select 옵션
    const memberOptions = members
        .map(function (m) { return '<option value="' + m.id + '">' + Page.esc(m.name) + '</option>'; })
        .join('');

    lastWeekDuties.forEach(function (d) {
        const statusClass = d.status;
        const statusLabel = d.status === 'DONE' ? '완료'
                          : d.status === 'MISSED' ? '미완료' : '대기중';
        let doneInfo = '';
        if (d.status === 'DONE') {
            doneInfo = ' (' + Page.esc(d.doneByName || '?') + ', ' + Page.shortDate(d.doneDate) + ')';
        }

        const html = '<div class="card lw-zone">'
            + '<div class="card-row">'
            + '  <span class="zone">' + Page.esc(d.zoneName) + '</span>'
            + '  <span class="status ' + statusClass + '">' + statusLabel + doneInfo + '</span>'
            + '</div>'
            + '<div class="lw-info">담당: ' + Page.esc(d.assigneeName) + '</div>'
            + '<div class="actions">'
            + '  <button class="btn-sm correct-done-btn" data-zone-id="' + d.zoneId + '">완료로 보정</button>'
            + '  <button class="btn-sm correct-missed-btn" data-zone-id="' + d.zoneId + '">미완료로 보정</button>'
            + '</div>'
            + '<div class="inline-form" id="done-form-' + d.zoneId + '">'
            + '  <select id="done-member-' + d.zoneId + '">' + memberOptions + '</select>'
            + '  <input type="date" id="done-date-' + d.zoneId + '"'
            + '         min="' + lastWeekDate + '" max="' + lastSunday + '"'
            + '         value="' + lastWeekDate + '">'
            + '  <button class="btn-sm primary confirm-done-btn" data-zone-id="' + d.zoneId + '">저장</button>'
            + '</div>'
            + '</div>';
        container.append(html);
    });
}

// ── 방 종료 ──────────────────────────────────────────────────

function closeRoom() {
    if (!confirm('방을 종료하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return;
    Page.clearMessage();
    Api.post('/api/rooms/close').then(function () {
        location.href = 'index.html';
    }).catch(function (err) { Page.showMessage(err.message); });
}
