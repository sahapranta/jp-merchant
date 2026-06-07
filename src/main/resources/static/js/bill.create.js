const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]').content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]').content;
const SERVER_ACCOUNTS = [];

// Fee catalogue defaults injected from server for current biller
// Loaded dynamically per section via /bills/api/fee-defaults?section=X
const SERVER_FEE_DEFAULTS = {};

/* ─────────────────────────────────────────────────────
   STATE
───────────────────────────────────────────────────── */
const S = {
    step:     1,
    adv:      false,
    section:  '',
    uniPM:    'ONE_TIME',
    months:   [],     
    feeRows:  [],
    semCnt:   0,
    draftId:  null,   // bill id after first draft save
};

const MONTH_NAMES = {
    JAN:'January',FEB:'February',MAR:'March',APR:'April',MAY:'May',JUN:'June',
    JUL:'July',AUG:'August',SEP:'September',OCT:'October',NOV:'November',DEC:'December'
};
const STEP_NAMES = ['','Bill Info','Classification','Fee Structure','Account Mapping','LPF / Fine','Preview & Publish'];

// Default fee names per section (fallback; replaced by API when available)
const DEF_FEES = {
    SCHOOL:           ['Tuition Fee','Session Fee','Exam Fee'],
    COLLEGE:          ['Tuition Fee','Session Fee','Exam Fee','Development Fund'],
    COLLEGE_SCIENCE:  ['Tuition Fee','Session Fee','Exam Fee','Lab Fee','Development Fund'],
    COLLEGE_COMMERCE: ['Tuition Fee','Session Fee','Exam Fee','Development Fund'],
    COLLEGE_ARTS:     ['Tuition Fee','Session Fee','Exam Fee'],
    UNIVERSITY:       ['Tuition Fee','Exam Fee','Registration Fee']
};

/* ─────────────────────────────────────────────────────
   CARD COLLAPSE
───────────────────────────────────────────────────── */
function toggleCard(bodyId, hdrId) {
    const body = document.getElementById(bodyId);
    const hdr  = document.getElementById(hdrId);
    if (!body) return;
    const closing = body.style.display !== 'none';
    body.style.display = closing ? 'none' : '';
    if (hdr) hdr.classList.toggle('closed', closing);
}

/* ─────────────────────────────────────────────────────
   ADVANCED MODE
───────────────────────────────────────────────────── */
$('#advToggle').on('change', function () {
    S.adv = this.checked;
    $('#modeLabel').text(this.checked ? 'Advanced Mode' : 'Simple Mode');
    $('body').toggleClass('adv-mode', this.checked);
    if (this.checked) { $('#simpleAcct').hide(); $('#advAcct').show(); if (S.step===4) buildAcctRows(); }
    else              { $('#simpleAcct').show(); $('#advAcct').hide(); }
});

/* ─────────────────────────────────────────────────────
   STEPPER
───────────────────────────────────────────────────── */
function goStep(n) {
    if (!validate(S.step)) return;
    $('#step' + S.step).hide();
    $('#step' + n).show();
    for (let i = 1; i <= 6; i++) {
        const el = $('#stp' + i);
        el.removeClass('active done');
        if (i < n)  el.addClass('done');
        if (i === n) el.addClass('active');
    }
    S.step = n;
    $('#footStep').text(n);
    $('#footName').text(STEP_NAMES[n]);
    if (n === 3) onEnterFee();
    if (n === 4) buildAcctRows();
    if (n === 5) { buildPerFeeLpf(); }
    if (n === 6) buildPreview();
    window.scrollTo({top:0, behavior:'smooth'});
}

/* ─────────────────────────────────────────────────────
   VALIDATION
───────────────────────────────────────────────────── */
function showErr(id, msg)  { const e=$('#'+id); if(msg) e.text(msg); e.addClass('show'); }
function clearErr(id)      { $('#'+id).removeClass('show'); }
function setErr(selId, errId, msg) { $('#'+selId).addClass('err'); showErr(errId,msg); return false; }
function setOk(selId, errId)       { $('#'+selId).removeClass('err'); clearErr(errId); }

function validate(step) {
    let ok = true;
    if (step === 1) {
        if (!$('#billTitle').val().trim()) { setErr('billTitle','e_title'); ok=false; } else setOk('billTitle','e_title');
        if (!$('#billType').val())         { setErr('billType','e_type'); ok=false;   } else setOk('billType','e_type');
        const sd=$('#billStart').val(), ed=$('#billEnd').val();
        if (!sd)         { setErr('billStart','e_start'); ok=false; } else setOk('billStart','e_start');
        if (!ed)         { setErr('billEnd','e_end','End date is required.'); ok=false; }
        else if(sd&&ed<sd){ setErr('billEnd','e_end','End date must be on or after start date.'); ok=false; }
        else setOk('billEnd','e_end');
    }
    if (step === 2) {
        if (!$('#instSection').val()) { setErr('instSection','e_section'); ok=false; } else setOk('instSection','e_section');
    }
    if (step === 3) {
        const needMonths = S.section !== 'UNIVERSITY' || S.uniPM === 'MONTHLY';
        if (needMonths && S.months.length === 0) { showErr('e_months'); ok=false; } else clearErr('e_months');
    }
    if (step === 4) {
        if (!S.adv) {
            if (!$('#singleAcct').val()) { setErr('singleAcct','e_acct'); ok=false; } else setOk('singleAcct','e_acct');
        } else {
            // Check all fee rows have an account mapped
            let allMapped = true;
            S.feeRows.forEach(row => { if (!row.accountId) allMapped = false; });
            if (!allMapped) { toast('Please assign a bank account to every fee item.','err'); ok=false; }
        }
    }
    if (!ok) {
        const first = $('.err, .errmsg.show').first();
        if (first.length) $('html,body').animate({scrollTop: first.offset().top - 130}, 250);
    }
    return ok;
}

/* ─────────────────────────────────────────────────────
   SECTION CHANGE
───────────────────────────────────────────────────── */
function onSection(val) {
    S.section = val;
    S.feeRows = [];
    S.months  = [];
    renderSelMonths();
    $('#hier_school,#hier_college,#hier_uni').hide();
    if (val === 'SCHOOL')     $('#hier_school').show();
    if (val === 'COLLEGE')    $('#hier_college').show();
    if (val === 'UNIVERSITY') $('#hier_uni').show();
}

function onCollegeGroup(val) {
    if (val === 'SCIENCE') $('#sciNotice').css('display','flex');
    else                   $('#sciNotice').hide();
}

function onFacultyChange(val) {    
    // Static demo departments
    const depts = {
        SCI:['Mathematics','Physics','Chemistry','Computer Science'],
        ARTS:['Bangla','English','History','Philosophy'],
        COM:['Accounting','Management','Finance'],
        ENG:['Civil Eng.','Electrical Eng.','Computer Sci. & Eng.'],
        default:['Select Faculty first']
    };
    const list = depts[val] || depts.default;
    const sel = $('#uniDept').empty().append('<option value="">— Select Department —</option>');
    list.forEach(d => sel.append(`<option value="${d.replace(/\s+/g,'_').toUpperCase()}">${d}</option>`));
}

/* ─────────────────────────────────────────────────────
   STEP 3 ENTRY
───────────────────────────────────────────────────── */
function onEnterFee() {
    if (S.section === 'UNIVERSITY') {
        $('#uniPayCard').show();
        applyUniPM(S.uniPM);
    } else {
        $('#uniPayCard').hide();
        $('#semCard').hide();
        $('#monthCard').show();
        if (S.feeRows.length === 0) seedRows();
        rebuildTable();
    }
}

function seedRows() {
    // Try to load from API; fall back to static defaults
    $.getJSON('/bills/api/fee-defaults?section=' + S.section, function(data) {
        if (data && data.length > 0) {
            S.feeRows = data.map(item => newRow(item.feeName, item.feeCode, item.accountHead, item.defaultAccountId, item.id));
        } else {
            useFallbackRows();
        }
        updateCount();
    }).fail(function() { useFallbackRows(); updateCount(); });
}

function useFallbackRows() {
    let key = S.section;
    if (S.section === 'COLLEGE') {
        const g = $('#collegeGroup').val();
        if (g==='SCIENCE') key='COLLEGE_SCIENCE'; else if(g==='COMMERCE') key='COLLEGE_COMMERCE'; else if(g==='ARTS') key='COLLEGE_ARTS';
    }
    const names = DEF_FEES[key] || DEF_FEES[S.section] || ['Tuition Fee'];
    S.feeRows = names.map(n => newRow(n));
}

function newRow(name, feeCode, accountHead, accountId, catId) {
    return {
        id: 'r' + Date.now() + Math.random().toString(36).slice(2,4),
        name:        name || 'New Fee',
        feeCode:     feeCode || '',
        accountHead: accountHead || '',
        accountId:   accountId || null,
        catId:       catId || null,
        amounts:     {}
    };
}

/* ─────────────────────────────────────────────────────
   UNI PAYMENT MODE
───────────────────────────────────────────────────── */
function selectPM(mode) {
    S.uniPM = mode;
    $('.pm-card').removeClass('on');
    $('#pm_' + mode).addClass('on');
    applyUniPM(mode);
}

function applyUniPM(mode) {
    if (mode === 'MONTHLY') {
        $('#monthCard').show(); $('#semCard').hide();
        if (S.feeRows.length === 0) {
            S.feeRows = DEF_FEES.UNIVERSITY.map(n => newRow(n));
        }
        rebuildTable();
    } else if (mode === 'SEMESTER') {
        $('#monthCard').hide(); $('#semCard').show();
        $('#ftblWrap').hide(); $('#noMonthMsg').hide();
        if ($('#semBlocks').children().length === 0) addSem();
    } else { // ONE_TIME
        $('#monthCard').hide(); $('#semCard').hide();
        if (S.months.length === 0) S.months = [{ label:'ONCE', display:'(One-time)', year:null, seq:1 }];
        if (S.feeRows.length === 0) {
            S.feeRows = DEF_FEES.UNIVERSITY.map(n => newRow(n));
        }
        rebuildTable();
        setTimeout(() => $('#ftblHead th:nth-child(2)').text('Amount (৳)'), 10);
    }
}

/* ─────────────────────────────────────────────────────
   MONTH MANAGEMENT
   Each month entry: { label:'JAN_2025', display:'Jan 2025', year:2025, seq:n }
   Pills use current year from #pillYear input.
   Custom month adds any month+year combination.
───────────────────────────────────────────────────── */
$(function() {
    const y = new Date().getFullYear();
    $('#pillYear').val(y);
    $('#cstYear').val(y);
    // Simple/adv sync
    $('#simpleAcct').show(); $('#advAcct').hide();

    if (typeof EXISTING_BILL !== 'undefined' && EXISTING_BILL && EXISTING_BILL.id) {
        initEditMode(EXISTING_BILL);
        // Don't default start date for edit; keep existing
    } else {
        $('#billStart').val(new Date().toISOString().split('T')[0]);
    }
});

$(document).on('click', '.mpill', function () {
    const m    = $(this).data('m');
    const yr   = parseInt($('#pillYear').val()) || new Date().getFullYear();
    const lbl  = m + '_' + yr;
    const disp = MONTH_NAMES[m].slice(0,3) + ' ' + yr;

    if ($(this).hasClass('on')) {
        // deselect — only remove entries for this month+year
        $(this).removeClass('on');
        S.months = S.months.filter(x => x.label !== lbl);
    } else {
        if (!S.months.find(x => x.label === lbl)) {
            S.months.push({ label:lbl, display:disp, year:yr, seq: S.months.length+1 });
        }
        $(this).addClass('on');
    }
    sortMonths();
    renderSelMonths();
    clearErr('e_months');
    rebuildTable();
});

function addCustomMonth() {
    const m    = $('#cstMonth').val();
    const yr   = parseInt($('#cstYear').val());
    if (!yr || yr < 2000 || yr > 2099) { toast('Enter a valid year.','err'); return; }
    const lbl  = m + '_' + yr;
    const disp = MONTH_NAMES[m].slice(0,3) + ' ' + yr;
    if (S.months.find(x => x.label === lbl)) { toast(disp + ' already added.','err'); return; }
    S.months.push({ label:lbl, display:disp, year:yr, seq: S.months.length+1 });
    sortMonths();
    renderSelMonths();
    clearErr('e_months');
    rebuildTable();
}

function removeMonth(lbl) {
    S.months = S.months.filter(x => x.label !== lbl);
    sortMonths();
    renderSelMonths();
    rebuildTable();
    // sync pill state
    const base = lbl.split('_')[0];
    const yr   = parseInt(lbl.split('_')[1]);
    if (yr === parseInt($('#pillYear').val())) {
        $(`.mpill[data-m="${base}"]`).removeClass('on');
    }
}

function sortMonths() {
    S.months.sort((a,b) => {
        if (a.year !== b.year) return (a.year||0) - (b.year||0);
        const mo = Object.keys(MONTH_NAMES);
        return mo.indexOf(a.label.split('_')[0]) - mo.indexOf(b.label.split('_')[0]);
    });
    S.months.forEach((m,i) => m.seq = i+1);
}

function renderSelMonths() {
    const c = $('#selMonthList').empty();
    S.months.forEach(m => {
        c.append(`<span class="badge d-flex align-items-center gap-1"
            style="background:var(--primary);font-size:.78rem;padding:.3rem .6rem;border-radius:20px">
            ${esc(m.display)}
            <i class="bi bi-x" style="cursor:pointer" onclick="removeMonth('${m.label}')"></i>
        </span>`);
    });
    $('#monthCount').text(S.months.length + ' selected');
    updateMonthPillStates();
}

function updateMonthPillStates() {
    // Sync pill highlight: a pill is ON if ANY entry with its base month and current year exists
    const yr = parseInt($('#pillYear').val()) || new Date().getFullYear();
    $('.mpill').each(function() {
        const m   = $(this).data('m');
        const lbl = m + '_' + yr;
        $(this).toggleClass('on', !!S.months.find(x => x.label === lbl));
    });
}

$('#pillYear').on('change', updateMonthPillStates);

function selAllMonths() {
    const yr = parseInt($('#pillYear').val()) || new Date().getFullYear();
    Object.keys(MONTH_NAMES).forEach(m => {
        const lbl = m + '_' + yr;
        if (!S.months.find(x => x.label === lbl)) {
            S.months.push({ label:lbl, display:MONTH_NAMES[m].slice(0,3)+' '+yr, year:yr, seq:0 });
        }
    });
    sortMonths(); renderSelMonths(); clearErr('e_months'); rebuildTable();
}

function clrMonths() {
    S.months = [];
    renderSelMonths();
    rebuildTable();
}

/* ─────────────────────────────────────────────────────
   FEE TABLE
───────────────────────────────────────────────────── */
function rebuildTable() {
    const ms = S.months;
    if (!ms.length) { $('#noMonthMsg').show(); $('#ftblWrap').hide(); return; }
    $('#noMonthMsg').hide(); $('#ftblWrap').show();

    $('#ftblHead th:not(:first-child)').remove();
    ms.forEach(m => $('#ftblHead').append(`<th>${esc(m.display)}</th>`));
    $('#ftblHead').append(`<th style="min-width:90px">Row Total</th><th style="width:36px"></th>`);

    $('#ftblBody').empty();
    S.feeRows.forEach(row => $('#ftblBody').append(rowHtml(row)));
    rebuildGrand();
}

function rowHtml(row) {
    let cells = '';
    S.months.forEach(m => {
        const v = row.amounts[m.label] != null ? row.amounts[m.label] : '';
        cells += `<td><input type="number" class="fee-in" min="0" step="0.01"
            data-rid="${row.id}" data-ml="${m.label}"
            value="${v}" placeholder="0"
            oninput="onCell(this,'${row.id}','${m.label}')"/></td>`;
    });
    return `<tr id="tr_${row.id}">
        <td>
            <div class="d-flex align-items-center gap-1 flex-wrap">
                <input type="text" class="form-control form-control-sm name-in"
                    value="${esc(row.name)}" placeholder="Fee name"
                    onchange="onName(this,'${row.id}')"/>
                <button type="button" class="fill-btn" onclick="fillRow('${row.id}')">Fill →</button>
            </div>
        </td>
        ${cells}
        <td><span class="rt-badge" id="rt_${row.id}">৳${fmt(calcRowTotal(row))}</span></td>
        <td><button type="button" class="ibtn del" onclick="removeRow('${row.id}')"><i class="bi bi-trash3"></i></button></td>
    </tr>`;
}

function rebuildGrand() {
    $('#grandRow td:not(:first-child)').remove();
    let grand = 0;
    S.months.forEach(m => {
        const cs = S.feeRows.reduce((s,r) => s+(parseFloat(r.amounts[m.label])||0), 0);
        grand += cs;
        $('#grandRow').append(`<td>৳${fmt(cs)}</td>`);
    });
    $('#grandRow').append(`<td>৳${fmt(grand)}</td><td></td>`);
}

function calcRowTotal(row) {
    return S.months.reduce((s,m) => s+(parseFloat(row.amounts[m.label])||0), 0);
}

function onCell(el, rid, ml) {
    const row = S.feeRows.find(r => r.id===rid);
    if (!row) return;
    row.amounts[ml] = parseFloat(el.value)||0;
    $(`#rt_${rid}`).text('৳'+fmt(calcRowTotal(row)));
    rebuildGrand();
}

function onName(el, rid) {
    const row = S.feeRows.find(r => r.id===rid);
    if (row) { row.name = el.value.trim(); buildAcctRows(); }
}

function fillRow(rid) {
    const row = S.feeRows.find(r => r.id===rid);
    if (!row) return;
    let seed = 0;
    for (const m of S.months) { if (row.amounts[m.label]) { seed=parseFloat(row.amounts[m.label]); break; } }
    if (!seed) seed = parseFloat($(`input[data-rid="${rid}"]`).first().val())||0;
    S.months.forEach(m => { row.amounts[m.label]=seed; });
    row._prop = true;
    $(`input[data-rid="${rid}"]`).val(seed).addClass('prop');
    $(`#rt_${rid}`).text('৳'+fmt(calcRowTotal(row)));
    rebuildGrand();
}

function addRow() {
    const row = newRow('New Fee');
    S.feeRows.push(row);
    if (S.months.length) {
        $('#ftblBody').append(rowHtml(row));
        rebuildGrand();
    }
    updateCount();
    $(`#tr_${row.id} .name-in`).focus().select();
    buildAcctRows();
}

function removeRow(rid) {
    S.feeRows = S.feeRows.filter(r=>r.id!==rid);
    $(`#tr_${rid}`).remove();
    rebuildGrand(); updateCount(); buildAcctRows();
}

function updateCount() {
    const n=S.feeRows.length;
    $('#feeCount').text(n+' item'+(n!==1?'s':''));
}

/* ─────────────────────────────────────────────────────
   SEMESTER BLOCKS
───────────────────────────────────────────────────── */
function addSem() {
    S.semCnt++;
    const i = S.semCnt;
    $('#semBlocks').append(`
    <div class="hier-box mb-3" id="sem_${i}">
        <div class="d-flex justify-content-between align-items-center mb-2">
            <strong style="font-size:.85rem">Semester ${i}</strong>
            <button type="button" class="ibtn del" onclick="$('#sem_${i}').remove()"><i class="bi bi-trash3"></i></button>
        </div>
        <div class="row g-2 mb-2">
            <div class="col-12">
                <label class="form-label">Semester Label <span class="req">*</span></label>
                <input type="text" class="form-control form-control-sm"
                    id="sem_${i}_name" placeholder="e.g. 1st Semester / Spring 2025"/>
            </div>
        </div>
        <div class="ftbl-wrap">
        <table class="ftbl" id="semtbl_${i}">
            <thead><tr>
                <th style="min-width:160px">Fee Item</th>
                <th>Amount (৳)</th>
                <th>Account</th>
                <th style="width:36px"></th>
            </tr></thead>
            <tbody id="semtbody_${i}"></tbody>
            <tfoot><tr id="semgrand_${i}">
                <td>Total</td><td id="semtotal_${i}">৳0</td><td></td><td></td>
            </tr></tfoot>
        </table>
        </div>
        <button type="button" class="btn btn-sm btn-outline-secondary mt-2"
            onclick="addSemFeeRow(${i})">
            <i class="bi bi-plus-lg me-1"></i>Add Fee Row
        </button>
    </div>`);
    // Add default university fee rows
    DEF_FEES.UNIVERSITY.forEach(n => addSemFeeRow(i, n));
}

function addSemFeeRow(semIdx, name) {
    const rid = 'sr_' + semIdx + '_' + Date.now();
    const nm  = name || 'New Fee';
    // Build account options from SERVER_ACCOUNTS or static fallback
    let acctOpts = '<option value="">— Account —</option>';
    if (SERVER_ACCOUNTS && SERVER_ACCOUNTS.length) {
        SERVER_ACCOUNTS.forEach(a => {
            acctOpts += `<option value="${a.id}">${esc(a.bankName)} — ${a.accountNo}</option>`;
        });
    } else {
        acctOpts += `<option value="1">DBBL ****1234</option><option value="2">Sonali Bank ****5678</option>`;
    }
    $(`#semtbody_${semIdx}`).append(`
    <tr id="${rid}">
        <td><input type="text" class="form-control form-control-sm name-in"
            value="${esc(nm)}" placeholder="Fee name"/></td>
        <td><input type="number" class="fee-in" min="0" step="0.01" placeholder="0"
            oninput="recalcSemTotal(${semIdx})"/></td>
        <td><select class="form-select form-select-sm" style="font-size:.78rem">${acctOpts}</select></td>
        <td><button type="button" class="ibtn del" onclick="$('#${rid}').remove();recalcSemTotal(${semIdx})">
            <i class="bi bi-trash3"></i></button></td>
    </tr>`);
    recalcSemTotal(semIdx);
}

function recalcSemTotal(semIdx) {
    let t = 0;
    $(`#semtbody_${semIdx} input[type=number]`).each(function() { t += parseFloat($(this).val())||0; });
    $(`#semtotal_${semIdx}`).text('৳'+fmt(t));
}

function removeSem(i) { $(`#sem_${i}`).remove(); }

/* ─────────────────────────────────────────────────────
   ACCOUNT MAPPING
───────────────────────────────────────────────────── */
function buildAcctRows() {
    if (!S.adv) return;
    const cont = $('#acctMapRows').empty();
    $('#acctSummary').hide();
    if (S.feeRows.length === 0) {
        cont.html('<p class="text-muted" style="font-size:.82rem">No fee items yet. Complete Step 3 first.</p>');
        return;
    }
    let grand = 0;
    S.feeRows.forEach(row => {
        const rt = calcRowTotal(row); grand += rt;
        let opts = '<option value="">— Select Account —</option>';
        const accounts = (SERVER_ACCOUNTS && SERVER_ACCOUNTS.length) ? SERVER_ACCOUNTS :
            [{id:1,bankName:'DBBL',accountNo:'****1234'},{id:2,bankName:'Sonali Bank',accountNo:'****5678'},{id:3,bankName:'Islami Bank',accountNo:'****9900'}];
        accounts.forEach(a => {
            const sel = row.accountId == a.id ? ' selected' : '';
            opts += `<option value="${a.id}"${sel}>${esc(a.bankName)} — ${a.accountNo}</option>`;
        });
        cont.append(`
        <div class="acct-row">
            <span class="acct-name"><i class="bi bi-dot"></i>${esc(row.name)}</span>
            <span class="acct-amt">৳${fmt(rt)}</span>
            <select class="form-select acct-sel" onchange="onAcctMap(this,'${row.id}')">${opts}</select>
        </div>`);
    });
    $('#mappedTotal').text('৳'+fmt(grand));
    $('#acctSummary').show();
    refreshAcctSummary();
}

function onAcctMap(el, rid) {
    const row = S.feeRows.find(r => r.id===rid);
    if (row) row.accountId = el.value ? parseInt(el.value) : null;
    refreshAcctSummary();
}

function refreshAcctSummary() {
    const totals = {}; const names = {};
    const accounts = (SERVER_ACCOUNTS && SERVER_ACCOUNTS.length) ? SERVER_ACCOUNTS :
        [{id:1,bankName:'DBBL',accountNo:'****1234'},{id:2,bankName:'Sonali Bank',accountNo:'****5678'}];
    accounts.forEach(a => { names[a.id] = a.bankName + ' — ' + a.accountNo; });
    S.feeRows.forEach(row => {
        if (row.accountId) totals[row.accountId] = (totals[row.accountId]||0) + calcRowTotal(row);
    });
    const sr = $('#acctSummaryRows').empty();
    Object.entries(totals).forEach(([k,v]) => {
        sr.append(`<div class="acct-summary-row">
            <span class="bank-lbl"><i class="bi bi-bank me-1"></i>${esc(names[k]||'Account '+k)}</span>
            <span class="bank-amt">৳${fmt(v)}</span>
        </div>`);
    });
}

/* ─────────────────────────────────────────────────────
   LPF
───────────────────────────────────────────────────── */
function toggleLpf(on) {
    if (on) {
        $('#lpfBox').slideDown(220);
        $('#lpfBadge').text('Enabled').css('background','rgba(25,135,84,.3)');
        const ed=$('#billEnd').val();
        if (ed && !$('#lpfStart').val()) {
            const d=new Date(ed); d.setDate(d.getDate()+1);
            $('#lpfStart').val(d.toISOString().split('T')[0]);
        }
    } else {
        $('#lpfBox').slideUp(220);
        $('#lpfBadge').text('Disabled').css('background','rgba(255,255,255,.22)');
    }
}

function onLpfType(val) {
    $('#lpfUnit').text((val==='PERCENT'||val==='DAILY_PCT')?'%':'৳');
}

function onLpfScope(val) {
    if (val==='PER_FEE') { buildPerFeeLpf(); $('#perFeeSection').show(); }
    else { $('#perFeeSection').hide(); }
}

function buildPerFeeLpf() {
    const c=$('#perFeeList').empty();
    S.feeRows.forEach(row => {
        c.append(`<div class="form-check">
            <input class="form-check-input" type="checkbox" checked
                id="lf_${row.id}" value="${row.id}"/>
            <label class="form-check-label" for="lf_${row.id}"
                style="font-size:.83rem">${esc(row.name)}</label>
        </div>`);
    });
}

/* ─────────────────────────────────────────────────────
   PREVIEW
───────────────────────────────────────────────────── */
function buildPreview() {
    const p = [];

    // Bill Info
    p.push(pvTitle('bi-file-earmark-text','Bill Information'));
    p.push(kv('Title',    gv('#billTitle')||'—'));
    p.push(kv('Type',     selTxt('#billType')||'—'));
    p.push(kv('Period',   (gv('#billStart')||'?')+' → '+(gv('#billEnd')||'?')));
    const ay=gv('#academicYear'); if(ay) p.push(kv('Academic Year',ay));

    // Classification
    p.push(pvTitle('bi-diagram-3','Classification'));
    p.push(kv('Section', selTxt('#instSection')||'—'));
    if(S.section==='SCHOOL'){
        p.push(kv('Class', selTxt('#schoolClass')||'—'));
        const sh=selTxt('#schoolShift'); if(sh&&sh!=='All Shifts') p.push(kv('Shift',sh));
        const sc=gv('#schoolSection'); if(sc) p.push(kv('Section',sc));
    }
    if(S.section==='COLLEGE'){
        p.push(kv('Year',  selTxt('#collegeYear')||'—'));
        p.push(kv('Group', selTxt('#collegeGroup')||'—'));
        const cs=gv('#collegeSection'); if(cs) p.push(kv('Section',cs));
    }
    if(S.section==='UNIVERSITY'){
        p.push(kv('Faculty',    selTxt('#uniFaculty')||'—'));
        p.push(kv('Department', selTxt('#uniDept')||'—'));
        p.push(kv('Program',    selTxt('#uniProgram')||'—'));
        p.push(kv('Year/Sem',   selTxt('#uniYearSem')||'—'));
        const ss=gv('#uniSession'); if(ss) p.push(kv('Session',ss));
        p.push(kv('Payment Mode', S.uniPM.replace('_',' ')));
    }

    // Fee Structure
    p.push(pvTitle('bi-table','Fee Structure'));
    if(S.section==='UNIVERSITY' && S.uniPM==='SEMESTER'){
        const rows=[];
        $('#semBlocks > div').each(function(){
            const lbl=$(this).find('[id$="_name"]').val()||'Semester';
            let total=0;
            const cells=[];
            $(this).find('input[type=number]').each(function(){ total+=parseFloat($(this).val())||0; });
            rows.push(`<tr><td>${esc(lbl)}</td><td>৳${fmt(total)}</td></tr>`);
        });
        if(rows.length){
            p.push(`<div class="ftbl-wrap"><table class="pv-tbl"><thead><tr><th>Semester</th><th>Total</th></tr></thead><tbody>${rows.join('')}</tbody></table></div>`);
        } else { p.push(pvWarn('No semester blocks configured.')); }
    } else if(S.feeRows.length && S.months.length){
        let tbl=`<div class="ftbl-wrap"><table class="pv-tbl"><thead><tr><th>Fee Item</th>`;
        S.months.forEach(m => tbl+=`<th>${esc(m.display)}</th>`);
        tbl+=`<th>Total</th></tr></thead><tbody>`;
        let grand=0; const cs={};
        S.months.forEach(m => cs[m.label]=0);
        S.feeRows.forEach(row => {
            tbl+=`<tr><td>${esc(row.name)}</td>`;
            S.months.forEach(m => {
                const a=parseFloat(row.amounts[m.label])||0;
                cs[m.label]+=a; tbl+=`<td>৳${fmt(a)}</td>`;
            });
            const rt=calcRowTotal(row); grand+=rt;
            tbl+=`<td>৳${fmt(rt)}</td></tr>`;
        });
        tbl+=`</tbody><tfoot><tr><td>Grand Total</td>`;
        S.months.forEach(m => tbl+=`<td>৳${fmt(cs[m.label])}</td>`);
        tbl+=`<td>৳${fmt(grand)}</td></tr></tfoot></table></div>`;
        p.push(tbl);
    } else { p.push(pvWarn('No fee items configured.')); }

    // Account Mapping
    p.push(pvTitle('bi-bank','Account Mapping'));
    if(!S.adv){
        p.push(kv('All fees via', selTxt('#singleAcct')||'—'));
    } else {
        S.feeRows.forEach(row => {
            const acctEl=$(`select.acct-sel`).filter(function(){ return $(this).closest('.acct-row').find('.acct-name').text().trim().endsWith(row.name); });
            p.push(kv(row.name, acctEl.length ? acctEl.find('option:selected').text() : '— not mapped —'));
        });
    }

    // LPF
    if($('#lpfToggle').is(':checked')){
        p.push(pvTitle('bi-exclamation-octagon','Late Payment Fine'));
        p.push(kv('Type',   selTxt('#lpfType')||'—'));
        p.push(kv('Amount', (gv('#lpfAmount')||'0')+' '+$('#lpfUnit').text()));
        p.push(kv('Scope',  selTxt('#lpfScope')||'—'));
        p.push(kv('Starts', gv('#lpfStart')||'—'));
        const le=gv('#lpfEnd'); p.push(kv('Ends', le||'No end date'));
    }

    p.push(`<div class="d-flex gap-2 flex-wrap mt-3">
        <span class="pv-badge"><i class="bi bi-buildings"></i>${esc(selTxt('#instSection')||'—')}</span>
        <span class="pv-badge"><i class="bi bi-tag"></i>${esc(selTxt('#billType')||'—')}</span>
        ${$('#lpfToggle').is(':checked')?'<span class="pv-badge"><i class="bi bi-exclamation-octagon"></i>LPF Enabled</span>':''}
    </div>`);

    $('#pvBody').html(p.join(''));
}

function pvTitle(ico,lbl){ return `<div class="pv-title"><i class="bi ${ico} me-1"></i>${lbl}</div>`; }
function pvWarn(msg){ return `<div class="pv-warn"><i class="bi bi-exclamation-triangle me-1"></i>${msg}</div>`; }
function kv(k,v){ return `<div class="pv-kv"><span class="k">${k}</span><span class="v">${esc(String(v))}</span></div>`; }

/* ─────────────────────────────────────────────────────
   COLLECT FORM DATA — builds BillRequest JSON for API
───────────────────────────────────────────────────── */
function collectFormData() {
    return {
        billTitle:      gv('#billTitle'),
        billType:       gv('#billType'),
        billStartDate:  gv('#billStart'),
        billEndDate:    gv('#billEnd'),
        academicYear:   gv('#academicYear'),
        billCode:       gv('#billCode'),
        billDescription: gv('#billDesc'),

        institutionSection: gv('#instSection'),
        schoolClass:    gv('#schoolClass'),
        shift:          gv('#schoolShift'),
        classSection:   gv('#schoolSection'),
        group:          gv('#schoolGroup'),
        version:        gv('#schoolVersion'),
        collegeYear:    gv('#collegeYear'),
        collegeGroup:   gv('#collegeGroup'),
        collegeSection: gv('#collegeSection'),
        faculty:        gv('#uniFaculty'),
        department:     gv('#uniDept'),
        program:        gv('#uniProgram'),
        yearSemester:   gv('#uniYearSem'),
        session:        gv('#uniSession'),
        uniPayMode:     S.uniPM,

        selectedMonths: S.months.map(m => ({ label:m.label, year:m.year, seq:m.seq })),

        feeRows: S.feeRows.map(row => ({
            catalogueItemId: row.catId,
            feeCode:         row.feeCode,
            feeName:         row.name,
            accountHead:     row.accountHead,
            bankAccountId:   S.adv ? row.accountId : parseInt(gv('#singleAcct')),
            amounts:         row.amounts,
            sortOrder:       S.feeRows.indexOf(row)
        })),

        singleAccountId: !S.adv ? parseInt(gv('#singleAcct')) : null,
        advancedMode:    S.adv,

        lpfEnabled:     $('#lpfToggle').is(':checked'),
        lpfStartDate:   gv('#lpfStart'),
        lpfEndDate:     gv('#lpfEnd'),
        lpfType:        gv('#lpfType'),
        lpfAmount:      parseFloat(gv('#lpfAmount'))||0,
        lpfScope:       gv('#lpfScope'),
        lpfMaxCap:      parseFloat(gv('#lpfCap'))||null,
        lpfGraceDays:   parseInt(gv('#lpfGrace'))||0,
        lpfRecurrence:  gv('#lpfRecur'),
        lpfWaiverRole:  gv('#lpfWaiver')
    };
}

function saveDraft() {
    const url    = S.draftId ? `/bills/${S.draftId}/draft` : '/bills/draft';
    const method = 'POST';
    const hdrs   = { 'Content-Type':'application/json' };
    hdrs[CSRF_HEADER] = CSRF_TOKEN;

    $.ajax({
        url, method,
        headers: hdrs,
        data: JSON.stringify(collectFormData()),
        contentType: 'application/json',
        success: function(res) {
            if (res.success) {
                S.draftId = res.billId;
                toast('Draft saved — ' + res.billCode, 'ok');
            } else {
                toast('Save failed: ' + (res.message||'Unknown error'), 'err');
            }
        },
        error: function(xhr) {
            toast('Save failed: ' + (xhr.responseJSON?.message || xhr.statusText), 'err');
        }
    });
}

function publishBill() {
    if (!S.draftId) {
        // Save draft first, then publish
        saveDraftThen(function() { doPublish(); });
        return;
    }
    doPublish();
}

function saveDraftThen(callback) {
    const url = '/bills/draft';
    const hdrs = { 'Content-Type':'application/json' };
    hdrs[CSRF_HEADER] = CSRF_TOKEN;
    $.ajax({
        url, method:'POST', headers:hdrs,
        data: JSON.stringify(collectFormData()),
        contentType: 'application/json',
        success: function(res) {
            if (res.success) { S.draftId=res.billId; callback(); }
            else toast('Could not save draft before publishing.','err');
        },
        error: function() { toast('Network error — could not save draft.','err'); }
    });
}

function doPublish() {
    const hdrs = {};
    hdrs[CSRF_HEADER] = CSRF_TOKEN;
    $.ajax({
        url:    '/bills/' + S.draftId + '/publish',
        method: 'POST',
        headers: hdrs,
        success: function(res) {
            if (res.success) {
                toast('Bill published! ' + res.billCode, 'ok');
                $('#publishBtn').prop('disabled', true).text('Published');
                setTimeout(() => window.location.href='/bills', 2500);
            } else {
                toast('Publish failed: '+(res.message||''), 'err');
            }
        },
        error: function(xhr) {
            toast('Publish failed: '+(xhr.responseJSON?.message||xhr.statusText),'err');
        }
    });
}

/* ─────────────────────────────────────────────────────
   TOAST
───────────────────────────────────────────────────── */
function toast(msg, type) {
    $('#_t').remove();
    const col = type==='ok'?'var(--ok)':'var(--danger)';
    const ico = type==='ok'?'bi-check-circle':'bi-x-circle';
    $('body').append(`<div id="_t" style="position:fixed;top:70px;right:20px;z-index:9999;
        background:#fff;border:1px solid var(--border);border-left:4px solid ${col};
        border-radius:8px;padding:.7rem 1.1rem;box-shadow:var(--shm);
        display:flex;align-items:center;gap:.6rem;font-size:.85rem;font-weight:500;
        animation:fadeInR .3s ease;">
        <i class="bi ${ico}" style="color:${col};font-size:1.1rem"></i>${esc(msg)}
    </div>`);
    setTimeout(()=>$('#_t').fadeOut(300,function(){$(this).remove();}),3500);
}

/* ─────────────────────────────────────────────────────
   UTILS
───────────────────────────────────────────────────── */
function fmt(n)      { return (parseFloat(n)||0).toLocaleString('en-IN',{minimumFractionDigits:0}); }
function esc(s)      { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
function gv(sel)     { return $(sel).val(); }
function selTxt(sel) { return $(sel+' option:selected').text().trim(); }

/* ─────────────────────────────────────────────────────
   INIT — populate form from existing bill for edit mode
───────────────────────────────────────────────────── */
function initEditMode(bill) {
    S.draftId = bill.id;

    $('#billTitle').val(bill.billTitle || '');
    $('#billType').val(bill.billType || '');
    $('#billStart').val(bill.billStartDate || '');
    $('#billEnd').val(bill.billEndDate || '');
    $('#academicYear').val(bill.academicYear || '');
    $('#billCode').val(bill.billCode || '');
    $('#billDesc').val(bill.billDescription || '');

    // Classification
    var cls = {};
    try { cls = JSON.parse(bill.classificationJson || '{}'); } catch (e) {}
    var section = bill.institutionSection || cls.section || '';
    if (section) {
        $('#instSection').val(section);
        onSection(section);
        switch (section) {
            case 'SCHOOL':
                $('#schoolClass').val(cls.schoolClass || '');
                $('#schoolShift').val(cls.shift || '');
                $('#schoolSection').val(cls.classSection || '');
                $('#schoolGroup').val(cls.group || '');
                $('#schoolVersion').val(cls.version || '');
                break;
            case 'COLLEGE':
                $('#collegeYear').val(cls.collegeYear || '');
                $('#collegeGroup').val(cls.collegeGroup || '');
                onCollegeGroup(cls.collegeGroup || '');
                $('#collegeSection').val(cls.collegeSection || '');
                $('#collegeShift').val(cls.shift || '');
                $('#collegeVersion').val(cls.version || '');
                break;
            case 'UNIVERSITY':
                $('#uniFaculty').val(cls.faculty || '');
                onFacultyChange(cls.faculty || '');
                $('#uniDept').val(cls.department || '');
                $('#uniProgram').val(cls.program || '');
                $('#uniYearSem').val(cls.yearSemester || '');
                $('#uniSession').val(cls.session || '');
                break;
        }
    }

    // University payment mode
    if (section === 'UNIVERSITY' && bill.uniPayMode) {
        S.uniPM = bill.uniPayMode;
    }

    // Restore months (onSection resets S.months, so populate after)
    S.months = (bill.months || []).map(function(m, i) {
        var label = m.label || '';
        var year = m.year;
        var display = label === 'ONCE' ? '(One-time)' :
            ((MONTH_NAMES[label.split('_')[0]] || label.split('_')[0]).slice(0, 3) + ' ' + (year || ''));
        return { label: label, year: year, display: display, seq: i + 1 };
    });

    // Restore fee rows
    S.feeRows = (bill.fees || []).map(function(f) {
        return {
            id: genId(),
            catId: f.id || null,
            feeCode: f.feeCode || '',
            name: f.feeName || '',
            accountHead: f.accountHead || '',
            accountId: f.bankAccountId || null,
            amounts: f.amounts || {}
        };
    });

    // Render months + fee table
    renderSelMonths();
    rebuildTable();

    // Account mapping: use first fee's account for simple mode
    if (S.feeRows.length > 0 && S.feeRows[0].accountId) {
        $('#singleAcct').val(S.feeRows[0].accountId);
    }

    // LPF
    if (bill.lpf) {
        var l = bill.lpf;
        if (l.enabled || l.isEnabled) {
            $('#lpfToggle').prop('checked', true);
            toggleLpf(true);
        }
        if (l.startDate)   $('#lpfStart').val(l.startDate);
        if (l.endDate)     $('#lpfEnd').val(l.endDate);
        if (l.fineType)    $('#lpfType').val(l.fineType);
        if (l.fineAmount)  $('#lpfAmount').val(l.fineAmount);
        if (l.fineScope)   $('#lpfScope').val(l.fineScope);
        if (l.maxCap)      $('#lpfCap').val(l.maxCap);
        if (l.graceDays)   $('#lpfGrace').val(l.graceDays);
        if (l.recurrence)  $('#lpfRecur').val(l.recurrence);
        if (l.waiverRole)  $('#lpfWaiver').val(l.waiverRole);
        onLpfType($('#lpfType').val());
        onLpfScope($('#lpfScope').val());
    }
}

var _idCounter = 0;
function genId() { return 'r' + (++_idCounter); }