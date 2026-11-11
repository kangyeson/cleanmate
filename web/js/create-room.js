/*
 * create-room.js — 방 만들기 화면 (R8)
 *
 * 사용하는 API
 *   GET  /api/auth/me   상단 메뉴용
 *   POST /api/rooms     {
 *                         roomName,
 *                         adminName,                         // members 중 한 명
 *                         members: [{ name, password }, ...], // 배열 순서 = 청소 순번
 *                         zones:   ["샤워실", ...]            // 배열 순서 = 구역 순서
 *                       }
 *                       → 201, 만든 관리자로 바로 로그인(CMSESSION 쿠키)
 *
 * 화면에서는 빈 값만 먼저 걸러 내고, 이름 길이·중복·비밀번호 길이 같은 최종 판단은 서버가 한다.
 * 진행 중인 방이 이미 있으면 서버가 409와 안내 문구를 돌려준다.
 */

$(function () {
    Api.me()
        .then(Page.renderHeader)
        .catch(function () { Page.renderHeader(null); });

    // 처음에는 룸메이트 4칸, 구역 4칸 (4인실 기준)
    for (let i = 0; i < 4; i++) addMemberRow();
    ['샤워실', '화장실', '세면대', '바닥'].forEach(addZoneRow);
    $('input[name=admin]').first().prop('checked', true);

    $('#add-member').on('click', function () { addMemberRow(); });
    $('#add-zone').on('click', function () { addZoneRow(''); });

    // 행 삭제 (최소 1행은 남김)
    $(document).on('click', '.remove-row', function () {
        const $list = $(this).closest('#member-rows, #zone-rows');
        if ($list.children().length > 1) {
            $(this).closest('.form-row').remove();
        }
    });

    $('#room-form').on('submit', function (e) {
        e.preventDefault();
        Page.clearMessage();

        const request = collect();
        if (!request) return;

        const $btn = $(this).find('button[type=submit]').prop('disabled', true);
        Api.post('/api/rooms', request)
            .then(function () {
                location.href = 'index.html';     // 생성 성공 → 관리자로 로그인된 이번 주 화면
            })
            .catch(function (err) {
                $btn.prop('disabled', false);
                Page.showMessage(err.message);
                window.scrollTo(0, 0);
            });
    });
});

function addMemberRow() {
    $('#member-rows').append(
        '<div class="form-row">'
        + '  <input type="text" class="m-name" placeholder="이름">'
        + '  <input type="password" class="m-password" placeholder="비밀번호" autocomplete="new-password">'
        + '  <label class="admin-pick"><input type="radio" name="admin"> 관리자</label>'
        + '  <button type="button" class="secondary remove-row" title="삭제">✕</button>'
        + '</div>'
    );
}

function addZoneRow(name) {
    $('#zone-rows').append(
        '<div class="form-row">'
        + '  <input type="text" class="z-name" placeholder="구역 이름" value="' + Page.esc(name) + '">'
        + '  <button type="button" class="secondary remove-row" title="삭제">✕</button>'
        + '</div>'
    );
}

/**
 * 입력값을 서버 요청 형식으로 모은다. 비어 있는 행은 건너뛴다.
 * 꼭 필요한 값이 빠졌으면 안내하고 null을 돌려준다.
 */
function collect() {
    const roomName = $('#room-name').val().trim();
    if (!roomName) {
        Page.showMessage('방 이름을 입력하세요.');
        return null;
    }

    const members = [];
    let adminName = null;
    $('#member-rows .form-row').each(function () {
        const name = $(this).find('.m-name').val().trim();
        const password = $(this).find('.m-password').val();
        if (!name && !password) return;                    // 완전히 빈 행은 무시
        members.push({ name: name, password: password });
        if ($(this).find('input[name=admin]').is(':checked')) adminName = name;
    });
    if (members.length === 0) {
        Page.showMessage('룸메이트를 1명 이상 입력하세요.');
        return null;
    }
    if (!adminName) {
        Page.showMessage('관리자를 고르세요. (이름이 입력된 행에서)');
        return null;
    }

    const zones = [];
    $('#zone-rows .z-name').each(function () {
        const zone = $(this).val().trim();
        if (zone) zones.push(zone);
    });
    if (zones.length === 0) {
        Page.showMessage('청소 구역을 1개 이상 입력하세요.');
        return null;
    }

    return { roomName: roomName, adminName: adminName, members: members, zones: zones };
}
