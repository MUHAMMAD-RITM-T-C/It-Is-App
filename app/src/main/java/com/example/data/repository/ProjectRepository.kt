package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProject(id: Long): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun getProjectDirect(id: Long): ProjectEntity? = projectDao.getProjectByIdDirect(id)

    suspend fun insert(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun update(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun delete(id: Long) = projectDao.deleteProjectById(id)

    suspend fun ensureDefaultProjectsExist() {
        if (projectDao.getProjectCount() == 0) {
            val starterProject = ProjectEntity(
                title = "کارت انیمیشنی مدرن",
                htmlCode = """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>HTML Code Studio</title>
</head>
<body>
  <div class="card-container">
    <div class="badge">🚀 کدنویسی با HTML Studio</div>
    <h1 id="title">سلام به دنیای وب!</h1>
    <p class="subtitle">اینجا می‌توانید کدهای HTML، CSS و جاوااسکریپت خود را به صورت زنده بنویسید و ویرایش کنید.</p>
    
    <div class="stats-grid">
      <div class="stat-box">
        <span class="stat-number" id="clickCount">0</span>
        <span class="stat-label">کلیک‌ها</span>
      </div>
      <div class="stat-box">
        <span class="stat-number">60fps</span>
        <span class="stat-label">پیش‌نمایش</span>
      </div>
    </div>

    <button id="actionBtn" class="action-btn">
      <span>کلیک برای افکت و رنگ</span>
    </button>
  </div>
</body>
</html>""",
                cssCode = """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

body {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #31104b 100%);
  color: #f8fafc;
  padding: 1.5rem;
}

.card-container {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 1.5rem;
  padding: 2.2rem;
  max-width: 420px;
  width: 100%;
  text-align: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  transition: transform 0.3s ease;
}

.card-container:hover {
  transform: translateY(-4px);
}

.badge {
  display: inline-block;
  padding: 0.35rem 0.9rem;
  background: rgba(249, 115, 22, 0.2);
  border: 1px solid rgba(249, 115, 22, 0.4);
  color: #fb923c;
  border-radius: 9999px;
  font-size: 0.85rem;
  margin-bottom: 1.2rem;
  font-weight: bold;
}

h1 {
  font-size: 1.8rem;
  font-weight: 800;
  margin-bottom: 0.8rem;
  background: linear-gradient(to right, #38bdf8, #818cf8, #c084fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #94a3b8;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1.8rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.8rem;
}

.stat-box {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 1rem;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.stat-number {
  display: block;
  font-size: 1.6rem;
  font-weight: bold;
  color: #38bdf8;
}

.stat-label {
  font-size: 0.8rem;
  color: #64748b;
}

.action-btn {
  width: 100%;
  padding: 0.95rem;
  border-radius: 1rem;
  border: none;
  background: linear-gradient(135deg, #f97316 0%, #ec4899 100%);
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 10px 25px -5px rgba(236, 72, 153, 0.4);
}

.action-btn:active {
  transform: scale(0.96);
}""",
                jsCode = """let clicks = 0;
const clickDisplay = document.getElementById('clickCount');
const actionBtn = document.getElementById('actionBtn');
const title = document.getElementById('title');

const colors = ['#38bdf8', '#4ade80', '#fbbf24', '#f472b6', '#a78bfa'];

actionBtn.addEventListener('click', () => {
  clicks++;
  clickDisplay.textContent = clicks;
  
  // Change button gradient and trigger haptic feel
  const randomColor = colors[clicks % colors.length];
  title.style.textShadow = `0 0 20px ${'$'}{randomColor}`;
  
  console.log('دکمه کلیک شد! تعداد کلیک‌ها: ' + clicks);
});
""",
                templateType = "starter"
            )
            insert(starterProject)
        }
    }
}
