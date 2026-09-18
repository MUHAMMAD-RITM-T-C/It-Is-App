package com.example.ui.editor.libraries

enum class LibraryCategory(val titleFa: String) {
    CSS_FRAMEWORKS("فریم‌ورک‌های CSS"),
    JAVASCRIPT("کتابخانه‌های جاوااسکریپت"),
    ICONS_FONTS("آیکون‌ها و فونت‌ها"),
    GRAPHICS_ANIMATION("گرافیک و انیمیشن")
}

data class CdnLibrary(
    val id: String,
    val name: String,
    val version: String,
    val category: LibraryCategory,
    val description: String,
    val tagSnippet: String,
    val isJs: Boolean = false
)

object CdnLibrariesCatalog {
    val libraries = listOf(
        CdnLibrary(
            id = "tailwind",
            name = "Tailwind CSS",
            version = "3.4.1",
            category = LibraryCategory.CSS_FRAMEWORKS,
            description = "فریم‌ورک محبوب مبتنی بر کلاس‌های کمکی برای طراحی سریع و مدرن",
            tagSnippet = """<script src="https://cdn.tailwindcss.com"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "bootstrap",
            name = "Bootstrap 5",
            version = "5.3.3",
            category = LibraryCategory.CSS_FRAMEWORKS,
            description = "محبوب‌ترین فریم‌ورک کامپوننت‌های ریسپانسیو وب",
            tagSnippet = """<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>""",
            isJs = false
        ),
        CdnLibrary(
            id = "fontawesome",
            name = "Font Awesome 6",
            version = "6.5.1",
            category = LibraryCategory.ICONS_FONTS,
            description = "مجموعه عظیم و کامل آیکون‌های وکتوری وب",
            tagSnippet = """<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">""",
            isJs = false
        ),
        CdnLibrary(
            id = "vazirmatn",
            name = "فونت ساحل / وزیرمتن",
            version = "Google Fonts",
            category = LibraryCategory.ICONS_FONTS,
            description = "فونت استاندارد، زیبا و چشم‌نواز فارسی از گوگل فونتز",
            tagSnippet = """<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@300;400;600;800&display=swap" rel="stylesheet">
<style>body { font-family: 'Vazirmatn', sans-serif; }</style>""",
            isJs = false
        ),
        CdnLibrary(
            id = "animate_css",
            name = "Animate.css",
            version = "4.1.1",
            category = LibraryCategory.GRAPHICS_ANIMATION,
            description = "مجموعه افکت‌ها و انیمیشن‌های آماده CSS آماده استفاده با یک کلاس",
            tagSnippet = """<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>""",
            isJs = false
        ),
        CdnLibrary(
            id = "confetti",
            name = "Canvas Confetti",
            version = "1.9.2",
            category = LibraryCategory.GRAPHICS_ANIMATION,
            description = "افکت جذاب پرتاب کاغذ رنگی و جشن با کنواس",
            tagSnippet = """<script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.2/dist/confetti.browser.min.js"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "sweetalert2",
            name = "SweetAlert2",
            version = "11.10",
            category = LibraryCategory.JAVASCRIPT,
            description = "دیالوگ‌ها و پاپ‌آپ‌های مدرن و زیبا با انیمیشن‌های روان",
            tagSnippet = """<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "chartjs",
            name = "Chart.js",
            version = "4.4.1",
            category = LibraryCategory.GRAPHICS_ANIMATION,
            description = "رسم نمودارهای گرافیکی تعاملی و زیبا (میله‌ای، دایره‌ای، خطی)",
            tagSnippet = """<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "jquery",
            name = "jQuery",
            version = "3.7.1",
            category = LibraryCategory.JAVASCRIPT,
            description = "کتابخانه نام‌آشنا برای کار آسان با المان‌های DOM و رویدادها",
            tagSnippet = """<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "vue",
            name = "Vue.js 3",
            version = "3.4.19",
            category = LibraryCategory.JAVASCRIPT,
            description = "فریم‌ورک سریع، پیشرو و واکنشی برای رابط کاربری وب",
            tagSnippet = """<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "threejs",
            name = "Three.js 3D",
            version = "r128",
            category = LibraryCategory.GRAPHICS_ANIMATION,
            description = "موتور گرافیک سه‌بعدی قدرتمند وب بر پایه WebGL",
            tagSnippet = """<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>""",
            isJs = true
        ),
        CdnLibrary(
            id = "axios",
            name = "Axios",
            version = "1.6.7",
            category = LibraryCategory.JAVASCRIPT,
            description = "ارسال درخواست‌های HTTP و ارتباط با APIها به صورت Promise",
            tagSnippet = """<script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>""",
            isJs = true
        )
    )
}
