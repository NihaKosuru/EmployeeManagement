// Simple front end for the Employee Management API (plain JavaScript, no frameworks)

const PAGE_SIZE = 5;
let authHeader = null;
let employeePage = 0;

// ---------- API helper ----------

async function api(method, url, body) {
    const options = {
        method: method,
        headers: {
            'Authorization': authHeader,
            // Tells Spring Security not to show the browser's own login popup
            'X-Requested-With': 'XMLHttpRequest'
        }
    };
    if (body) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);
    if (response.status === 204) {
        return null;
    }
    if (response.status === 401) {
        throw new Error('Wrong username or password.');
    }
    if (response.status === 403) {
        throw new Error('You do not have permission. Log in as admin to make changes.');
    }

    const data = await response.json();
    if (!response.ok) {
        // Error body from GlobalExceptionHandler
        let message = data.message;
        if (data.fieldErrors) {
            message += ': ' + Object.values(data.fieldErrors).join(', ');
        }
        throw new Error(message);
    }
    return data;
}

// ---------- Small helpers ----------

function byId(id) {
    return document.getElementById(id);
}

function showMessage(text, isError) {
    const box = byId('message');
    box.textContent = text;
    box.className = isError ? 'message error' : 'message';
    window.scrollTo(0, 0);
}

function hideMessage() {
    byId('message').className = 'message hidden';
}

function formatMoney(value) {
    return '₹ ' + Number(value).toLocaleString('en-IN');
}

// Runs an action and shows any error on the page
async function run(action, successMessage) {
    try {
        await action();
        if (successMessage) {
            showMessage(successMessage, false);
        }
    } catch (error) {
        showMessage(error.message, true);
    }
}

// Adds a table row. textContent is used so data is never treated as HTML.
function addRow(tableBody, values, buttons) {
    const row = tableBody.insertRow();
    values.forEach(value => {
        row.insertCell().textContent = value ?? '';
    });
    if (buttons) {
        const cell = row.insertCell();
        buttons.forEach(([label, onClick, cssClass]) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = label;
            button.className = 'small ' + (cssClass || '');
            button.onclick = onClick;
            cell.appendChild(button);
        });
    }
}

function fillSelect(select, items, getLabel, firstOption) {
    const selected = select.value;
    select.innerHTML = '';
    if (firstOption) {
        select.add(new Option(firstOption, ''));
    }
    items.forEach(item => select.add(new Option(getLabel(item), item.id)));
    select.value = selected;
}

// ---------- Login ----------

byId('login-form').addEventListener('submit', async event => {
    event.preventDefault();
    const username = byId('username').value;
    authHeader = 'Basic ' + btoa(username + ':' + byId('password').value);
    let currentUser;
    try {
        currentUser = await api('GET', '/api/me');
    } catch (error) {
        authHeader = null;
        showMessage(error.message, true);
        return;
    }
    hideMessage();
    // Hide add/edit/delete controls for read-only users
    document.body.classList.toggle('read-only', currentUser.role !== 'ADMIN');
    byId('login-section').classList.add('hidden');
    byId('app').classList.remove('hidden');
    byId('user-info').classList.remove('hidden');
    byId('logged-in-as').textContent = 'Logged in as ' + currentUser.username + ' (' + currentUser.role + ')';
    await run(loadEverything);
});

function logout() {
    authHeader = null;
    document.body.classList.remove('read-only');
    resetEmployeeForm();
    // Start the next login on the first page of the Employees tab
    employeePage = 0;
    document.querySelector('.tab[data-tab=employees]').click();
    byId('password').value = '';
    byId('app').classList.add('hidden');
    byId('user-info').classList.add('hidden');
    byId('login-section').classList.remove('hidden');
    hideMessage();
}

async function loadEverything() {
    await loadDepartments();
    await loadProjects();
    await loadEmployees();
}

// ---------- Tabs ----------

document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(s => s.classList.add('hidden'));
        tab.classList.add('active');
        byId(tab.dataset.tab).classList.remove('hidden');
        hideMessage();

        if (tab.dataset.tab === 'assignments') {
            run(loadAssignments);
        } else if (tab.dataset.tab === 'reports') {
            run(loadReports);
        }
    });
});

// ---------- Employees ----------

async function loadEmployees() {
    const params = new URLSearchParams({
        page: employeePage,
        size: PAGE_SIZE,
        sortBy: byId('search-sort').value
    });
    if (byId('search-name').value.trim()) {
        params.append('name', byId('search-name').value.trim());
    }
    if (byId('search-department').value) {
        params.append('departmentId', byId('search-department').value);
    }

    const data = await api('GET', '/api/employees?' + params);
    const table = byId('employee-table');
    table.innerHTML = '';
    data.content.forEach(employee => {
        addRow(table,
            [employee.id, employee.name, employee.email, employee.designation, formatMoney(employee.salary), employee.departmentName],
            [['Edit', () => editEmployee(employee), 'admin-only'],
             ['Delete', () => deleteEmployee(employee), 'danger admin-only']]);
    });

    const totalPages = Math.max(data.totalPages, 1);
    byId('page-info').textContent =
        'Page ' + (data.page + 1) + ' of ' + totalPages + ' (' + data.totalElements + ' employees)';
    byId('previous-page').disabled = data.page === 0;
    byId('next-page').disabled = data.page + 1 >= totalPages;
}

byId('employee-search').addEventListener('submit', event => {
    event.preventDefault();
    employeePage = 0;
    run(loadEmployees);
});

byId('previous-page').addEventListener('click', () => {
    employeePage--;
    run(loadEmployees);
});

byId('next-page').addEventListener('click', () => {
    employeePage++;
    run(loadEmployees);
});

function editEmployee(employee) {
    byId('employee-id').value = employee.id;
    byId('employee-version').value = employee.version;
    byId('employee-name').value = employee.name;
    byId('employee-email').value = employee.email;
    byId('employee-designation').value = employee.designation;
    byId('employee-salary').value = employee.salary;
    byId('employee-department').value = employee.departmentId;
    byId('employee-form-title').textContent = 'Edit Employee';
    byId('employee-save-button').textContent = 'Update';
    window.scrollTo(0, 0);
}

function resetEmployeeForm() {
    byId('employee-form').reset();
    byId('employee-id').value = '';
    byId('employee-version').value = '';
    byId('employee-form-title').textContent = 'Add Employee';
    byId('employee-save-button').textContent = 'Add';
}

byId('employee-form').addEventListener('submit', event => {
    event.preventDefault();
    const id = byId('employee-id').value;
    const employee = {
        name: byId('employee-name').value,
        email: byId('employee-email').value,
        designation: byId('employee-designation').value,
        salary: byId('employee-salary').value,
        departmentId: byId('employee-department').value,
        // The version read when Edit was clicked. If someone else saved the
        // employee in the meantime, the server answers 409 Conflict.
        version: id ? byId('employee-version').value : null
    };
    run(async () => {
        if (id) {
            await api('PUT', '/api/employees/' + id, employee);
        } else {
            await api('POST', '/api/employees', employee);
        }
        resetEmployeeForm();
        await loadEmployees();
    }, id ? 'Employee updated.' : 'Employee added.');
});

function deleteEmployee(employee) {
    if (!confirm('Delete ' + employee.name + '?')) {
        return;
    }
    run(async () => {
        await api('DELETE', '/api/employees/' + employee.id);
        await loadEmployees();
    }, 'Employee deleted.');
}

// ---------- Departments ----------

async function loadDepartments() {
    const departments = await api('GET', '/api/departments');
    const table = byId('department-table');
    table.innerHTML = '';
    departments.forEach(department => {
        addRow(table, [department.name, department.location],
            [['Employees', () => showDepartmentEmployees(department)],
             ['Delete', () => deleteDepartment(department), 'danger admin-only']]);
    });
    fillSelect(byId('employee-department'), departments, d => d.name, 'Select department');
    fillSelect(byId('search-department'), departments, d => d.name, 'All departments');
}

function showDepartmentEmployees(department) {
    run(async () => {
        const employees = await api('GET', '/api/departments/' + department.id + '/employees');
        const names = employees.map(e => e.name).join(', ') || 'none';
        byId('department-members').textContent = 'Employees in ' + department.name + ': ' + names;
    });
}

byId('department-form').addEventListener('submit', event => {
    event.preventDefault();
    const department = {
        name: byId('department-name').value,
        location: byId('department-location').value
    };
    run(async () => {
        await api('POST', '/api/departments', department);
        byId('department-form').reset();
        await loadDepartments();
    }, 'Department added.');
});

function deleteDepartment(department) {
    if (!confirm('Delete department ' + department.name + '?')) {
        return;
    }
    run(async () => {
        await api('DELETE', '/api/departments/' + department.id);
        await loadDepartments();
    }, 'Department deleted.');
}

// ---------- Projects ----------

async function loadProjects() {
    const projects = await api('GET', '/api/projects');
    const table = byId('project-table');
    table.innerHTML = '';
    projects.forEach(project => {
        addRow(table, [project.name, project.description, formatMoney(project.budget)],
            [['Employees', () => showProjectEmployees(project)],
             ['Delete', () => deleteProject(project), 'danger admin-only']]);
    });
    fillSelect(byId('assignment-project'), projects, p => p.name, 'Select project');
}

function showProjectEmployees(project) {
    run(async () => {
        const employees = await api('GET', '/api/projects/' + project.id + '/employees');
        const names = employees.map(e => e.name).join(', ') || 'none';
        byId('project-members').textContent = 'Employees on ' + project.name + ': ' + names;
    });
}

byId('project-form').addEventListener('submit', event => {
    event.preventDefault();
    const project = {
        name: byId('project-name').value,
        description: byId('project-description').value,
        budget: byId('project-budget').value
    };
    run(async () => {
        await api('POST', '/api/projects', project);
        byId('project-form').reset();
        await loadProjects();
    }, 'Project added.');
});

function deleteProject(project) {
    if (!confirm('Delete project ' + project.name + '?')) {
        return;
    }
    run(async () => {
        await api('DELETE', '/api/projects/' + project.id);
        await loadProjects();
    }, 'Project deleted.');
}

// ---------- Assignments ----------

async function loadAssignments() {
    const employees = await api('GET', '/api/employees?size=100&sortBy=name');
    fillSelect(byId('assignment-employee'), employees.content, e => e.name, 'Select employee');
    await loadProjects();

    const assignments = await api('GET', '/api/assignments');
    const table = byId('assignment-table');
    table.innerHTML = '';
    assignments.forEach(assignment => {
        addRow(table,
            [assignment.employeeName, assignment.projectName, assignment.role, assignment.assignedDate],
            [['Remove', () => removeAssignment(assignment), 'danger admin-only']]);
    });
}

byId('assignment-form').addEventListener('submit', event => {
    event.preventDefault();
    const assignment = {
        employeeId: byId('assignment-employee').value,
        projectId: byId('assignment-project').value,
        role: byId('assignment-role').value
    };
    run(async () => {
        await api('POST', '/api/assignments', assignment);
        byId('assignment-role').value = '';
        await loadAssignments();
    }, 'Employee assigned to project.');
});

function removeAssignment(assignment) {
    if (!confirm('Remove ' + assignment.employeeName + ' from ' + assignment.projectName + '?')) {
        return;
    }
    run(async () => {
        await api('DELETE', '/api/assignments/' + assignment.id);
        await loadAssignments();
    }, 'Assignment removed.');
}

// ---------- Reports ----------

async function loadReports() {
    const departmentRows = await api('GET', '/api/reports/department-summary');
    const departmentTable = byId('department-report');
    departmentTable.innerHTML = '';
    departmentRows.forEach(row => {
        addRow(departmentTable, [row.departmentName, row.employeeCount, formatMoney(row.totalSalary)]);
    });

    const projectRows = await api('GET', '/api/reports/project-summary');
    const projectTable = byId('project-report');
    projectTable.innerHTML = '';
    projectRows.forEach(row => {
        addRow(projectTable, [row.projectName, formatMoney(row.budget), row.employeeCount]);
    });
}
