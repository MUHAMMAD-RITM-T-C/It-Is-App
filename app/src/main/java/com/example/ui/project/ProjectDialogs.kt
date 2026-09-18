package com.example.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListSheet(
    projects: List<ProjectEntity>,
    currentProjectId: Long,
    onSelectProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onDuplicateProject: (ProjectEntity) -> Unit,
    onToggleFavorite: (ProjectEntity) -> Unit,
    onOpenNewProjectDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filteredProjects = projects.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }.sortedWith(compareByDescending<ProjectEntity> { it.isFavorite }.thenByDescending { it.updatedAt })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "مدیریت پروژه‌ها",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenNewProjectDialog,
                    modifier = Modifier.testTag("create_new_project_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پروژه جدید")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_search_field"),
                placeholder = { Text("جستجو در بین پروژه‌ها...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Projects List
            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ پروژه‌ای یافت نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        val isCurrent = project.id == currentProjectId
                        val formattedDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            .format(Date(project.updatedAt))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectProject(project.id)
                                    onDismiss()
                                }
                                .testTag("project_item_${project.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = project.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "فعال",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "آخرین تغییر: $formattedDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Favorite
                                    IconButton(onClick = { onToggleFavorite(project) }) {
                                        Icon(
                                            imageVector = if (project.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "نشان کردن",
                                            tint = if (project.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Duplicate
                                    IconButton(onClick = { onDuplicateProject(project) }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "تکثیر پروژه",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete
                                    IconButton(onClick = { projectToDelete = project }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف پروژه",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("حذف پروژه") },
            text = { Text("آیا مطمئن هستید که می‌خواهید پروژه «${project.title}» را برای همیشه حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(project.id)
                        projectToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_project_btn")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

data class TemplateInfo(
    val id: String,
    val title: String,
    val description: String,
    val htmlCode: String,
    val cssCode: String = "",
    val jsCode: String = ""
)

val STARTER_TEMPLATES = listOf(
    TemplateInfo(
        id = "blank",
        title = "پروژه خالی (Blank HTML5)",
        description = "یک قالب اولیه تمیز و استاندارد HTML5 برای شروع از صفر",
        htmlCode = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>پروژه جدید</title>
</head>
<body>
  <h1>سلام دنیا!</h1>
  <p>شروع به نوشتن کدهای وب خود کنید...</p>
</body>
</html>""",
        cssCode = """body {
  font-family: sans-serif;
  padding: 2rem;
  background-color: #f8fafc;
  color: #0f172a;
}
h1 {
  color: #2563eb;
}""",
        jsCode = """console.log("پروژه با موفقیت بارگذاری شد!");"""
    ),
    TemplateInfo(
        id = "tailwind",
        title = "قالب مدرن با Tailwind CSS",
        description = "طراحی کارت مدرن با پالت رنگی جذاب، افکت شیشه‌ای و تیل‌ویند",
        htmlCode = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <script src="https://cdn.tailwindcss.com"></script>
  <title>کارت تیل‌ویند</title>
</head>
<body class="bg-slate-900 min-h-screen flex items-center justify-center p-4">
  <div class="bg-slate-800/80 backdrop-blur border border-slate-700 rounded-2xl p-6 max-w-sm w-full text-center shadow-2xl">
    <div class="w-16 h-16 bg-gradient-to-tr from-amber-500 to-rose-500 rounded-2xl mx-auto mb-4 flex items-center justify-center text-white text-2xl font-black">
      HTML
    </div>
    <h2 class="text-xl font-bold text-white mb-2">طراحی با Tailwind</h2>
    <p class="text-slate-400 text-sm mb-6">از تمامی کلاس‌های Utility-first تیل‌ویند در این ویرایشگر بدون نیاز به نصب لذت ببرید!</p>
    <button id="likeBtn" class="w-full bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-semibold py-2.5 px-4 rounded-xl transition duration-200 shadow-lg shadow-cyan-500/25">
      ❤️ پسندیدن (<span id="likes">0</span>)
    </button>
  </div>
</body>
</html>""",
        cssCode = "",
        jsCode = """let likes = 0;
const btn = document.getElementById('likeBtn');
const counter = document.getElementById('likes');

btn.addEventListener('click', () => {
  likes++;
  counter.textContent = likes;
});"""
    ),
    TemplateInfo(
        id = "game",
        title = "بازی رفلکس کنواس (HTML5 Canvas Game)",
        description = "بازی ساده و تعاملی کلیک روی دایره‌ها با رندر روی Canvas",
        htmlCode = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>بازی رفلکس</title>
</head>
<body>
  <div id="game-ui">
    <div class="score-card">امتیاز: <span id="score">0</span></div>
    <p class="hint">روی دایره‌های در حال حرکت سریع کلیک کن!</p>
  </div>
  <canvas id="gameCanvas"></canvas>
</body>
</html>""",
        cssCode = """* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  background: #090d16;
  color: #fff;
  font-family: sans-serif;
  overflow: hidden;
  text-align: center;
}
#game-ui {
  position: absolute;
  top: 15px;
  width: 100%;
  pointer-events: none;
}
.score-card {
  font-size: 24px;
  font-weight: bold;
  color: #38bdf8;
}
.hint {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}
canvas {
  display: block;
  width: 100vw;
  height: 100vh;
}""",
        jsCode = """const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const scoreEl = document.getElementById('score');

canvas.width = window.innerWidth;
canvas.height = window.innerHeight;

let score = 0;
let target = {
  x: canvas.width / 2,
  y: canvas.height / 2,
  radius: 35,
  color: '#f43f5e'
};

function draw() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.beginPath();
  ctx.arc(target.x, target.y, target.radius, 0, Math.PI * 2);
  ctx.fillStyle = target.color;
  ctx.shadowColor = target.color;
  ctx.shadowBlur = 15;
  ctx.fill();
  ctx.shadowBlur = 0;
}

canvas.addEventListener('click', (e) => {
  const dist = Math.hypot(e.clientX - target.x, e.clientY - target.y);
  if (dist < target.radius) {
    score += 10;
    scoreEl.textContent = score;
    target.x = Math.random() * (canvas.width - 80) + 40;
    target.y = Math.random() * (canvas.height - 140) + 70;
    const colors = ['#f43f5e', '#ec4899', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b'];
    target.color = colors[Math.floor(Math.random() * colors.length)];
    draw();
  }
});

draw();
"""
    ),
    TemplateInfo(
        id = "calculator",
        title = "ماشین حساب تعاملی (Calculator)",
        description = "ماشین حساب کارآمد و زیبا با طراحی مدرن گلاسمورفیسم",
        htmlCode = """<!DOCTYPE html>
<html lang="fa">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ماشین حساب</title>
</head>
<body>
  <div class="calc-body">
    <div id="display" class="calc-screen">0</div>
    <div class="keys-grid">
      <button class="op-btn" onclick="clearScreen()">C</button>
      <button class="op-btn" onclick="appendChar('/')">÷</button>
      <button class="op-btn" onclick="appendChar('*')">×</button>
      <button class="op-btn" onclick="appendChar('-')">−</button>
      
      <button onclick="appendChar('7')">7</button>
      <button onclick="appendChar('8')">8</button>
      <button onclick="appendChar('9')">9</button>
      <button class="op-btn" onclick="appendChar('+')">+</button>
      
      <button onclick="appendChar('4')">4</button>
      <button onclick="appendChar('5')">5</button>
      <button onclick="appendChar('6')">6</button>
      <button class="eq-btn" onclick="calculate()">=</button>
      
      <button onclick="appendChar('1')">1</button>
      <button onclick="appendChar('2')">2</button>
      <button onclick="appendChar('3')">3</button>
      <button onclick="appendChar('0')">0</button>
    </div>
  </div>
</body>
</html>""",
        cssCode = """body {
  background: #0f172a;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: sans-serif;
  margin: 0;
}
.calc-body {
  background: #1e293b;
  padding: 1.5rem;
  border-radius: 1.5rem;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4);
  width: 300px;
}
.calc-screen {
  background: #090d16;
  color: #38bdf8;
  font-size: 2rem;
  text-align: right;
  padding: 1rem;
  border-radius: 0.8rem;
  margin-bottom: 1.2rem;
  min-height: 60px;
  overflow-x: auto;
}
.keys-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.7rem;
}
button {
  padding: 1rem;
  border-radius: 0.8rem;
  border: none;
  background: #334155;
  color: white;
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
}
button:active { opacity: 0.7; }
.op-btn { background: #f59e0b; }
.eq-btn { background: #10b981; grid-row: span 2; }
""",
        jsCode = """let expr = '';
const screen = document.getElementById('display');

function appendChar(c) {
  expr += c;
  screen.textContent = expr;
}

function clearScreen() {
  expr = '';
  screen.textContent = '0';
}

function calculate() {
  try {
    expr = String(Function('"use strict";return (' + expr + ')')());
    screen.textContent = expr;
  } catch(e) {
    screen.textContent = 'خطا!';
    expr = '';
  }
}
"""
    )
)

@Composable
fun NewProjectDialog(
    onConfirm: (title: String, template: TemplateInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(STARTER_TEMPLATES[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ایجاد پروژه جدید",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("نام پروژه") },
                    placeholder = { Text("مثلاً: سایت شخصی من") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "انتخاب قالب اولیه:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    STARTER_TEMPLATES.forEach { template ->
                        val isSelected = template.id == selectedTemplate.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        Color.Transparent
                                )
                                .clickable { selectedTemplate = template }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTemplate = template }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifBlank { selectedTemplate.title }
                    onConfirm(finalTitle, selectedTemplate)
                },
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) {
                Text("ایجاد پروژه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
