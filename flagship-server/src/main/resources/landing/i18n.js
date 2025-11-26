// Internationalization for Flagship Landing Page

const translations = {
    ru: {
        title: "Flagship - Feature Flags & A/B Testing для Kotlin Multiplatform",
        "nav.features": "Возможности",
        "nav.pricing": "Тарифы",
        "nav.docs": "Документация",
        "nav.blog": "Блог",
        "nav.about": "О проекте",
        "nav.admin": "Войти в админку",
        "hero.badge": "🚧 В стадии активной разработки",
        "hero.title": "Feature Flags & A/B Testing для Kotlin Multiplatform",
        "hero.subtitle": "Простая интеграция, типобезопасность, realtime обновления. Self-hosted или облако — выбор за вами.",
        "hero.getStarted": "Начать работу",
        "hero.stats.kmp": "Kotlin Multiplatform",
        "hero.stats.integration": "Интеграция",
        "hero.stats.opensource": "Open Source SDK",
        "features.title": "Возможности",
        "deployment.title": "Варианты развертывания",
        "deployment.cloud.title": "Облако (SaaS)",
        "deployment.cloud.desc": "Готовое решение без настройки инфраструктуры. Просто подключите SDK и начните работу.",
        "deployment.cloud.feature1": "Автоматические обновления",
        "deployment.cloud.feature2": "Масштабирование из коробки",
        "deployment.cloud.feature3": "Мониторинг и аналитика",
        "deployment.cloud.feature4": "Техническая поддержка",
        "deployment.cloud.status": "Скоро",
        "deployment.selfhosted.title": "Self-hosted",
        "deployment.selfhosted.desc": "Полный контроль над данными. Развертывание на вашей инфраструктуре.",
        "deployment.selfhosted.feature1": "Docker-compose setup",
        "deployment.selfhosted.feature2": "PostgreSQL база данных",
        "deployment.selfhosted.feature3": "Админ-панель",
        "deployment.selfhosted.feature4": "REST API",
        "deployment.selfhosted.status": "Доступно",
        "deployment.selfhosted.docs": "Документация",
        "pricing.title": "Тарифы",
        "pricing.simple.title": "Цены в разработке",
        "pricing.simple.desc": "Проект находится в активной стадии разработки. Цены и тарифы будут определены позже.",
        "pricing.simple.open": "Мы открыты к предложениям по:",
        "pricing.simple.item1": "🤝 Сотрудничеству и партнёрству",
        "pricing.simple.item2": "📢 Продвижению и маркетингу",
        "pricing.simple.item3": "💻 Технической помощи и контрибуциям",
        "pricing.simple.item4": "💬 Обратной связи и предложениям",
        "pricing.simple.contact": "Свяжитесь с нами через GitHub или Issues, чтобы обсудить возможности сотрудничества!",
        "pricing.simple.button": "Связаться",
        "docs.title": "Документация",
        "docs.guide": "Руководство",
        "docs.guideDesc": "Полное руководство по использованию SDK",
        "docs.api": "API Reference",
        "docs.apiDesc": "Документация REST API",
        "docs.migration": "Migration Guide",
        "docs.migrationDesc": "Руководство по миграции",
        "docs.githubDesc": "Исходный код и issues",
        "blog.title": "Блог",
        "blog.subtitle": "Лучшие практики и статьи о feature flags, A/B тестировании и процессе разработки",
        "blog.article1.title": "Лучшие практики работы с Feature Flags",
        "blog.article1.desc": "Руководство по эффективному использованию feature flags в процессе разработки",
        "blog.article1.en": "🇬🇧 English",
        "blog.article1.ru": "🇷🇺 Русский",
        "blog.article2.title": "Процесс разработки с Feature Flags",
        "blog.article2.desc": "Как интегрировать feature flags в CI/CD и процесс разработки",
        "blog.article2.en": "🇬🇧 English",
        "blog.article2.ru": "🇷🇺 Русский",
        "blog.article3.title": "A/B тестирование с Feature Flags",
        "blog.article3.desc": "Проведение экспериментов и анализ результатов с помощью feature flags",
        "blog.article3.en": "🇬🇧 English",
        "blog.article3.ru": "🇷🇺 Русский",
        "blog.article4.title": "Безопасность и управление Feature Flags",
        "blog.article4.desc": "Практики безопасности, управление жизненным циклом и kill switches",
        "blog.article4.en": "🇬🇧 English",
        "blog.article4.ru": "🇷🇺 Русский",
        "blog.article5.title": "Мониторинг и аналитика Feature Flags",
        "blog.article5.desc": "Отслеживание использования флагов, метрики и дашборды",
        "blog.article5.en": "🇬🇧 English",
        "blog.article5.ru": "🇷🇺 Русский",
        "blog.article6.title": "Gradual Rollouts и Canary Releases",
        "blog.article6.desc": "Постепенное развертывание новых функций с минимальными рисками",
        "blog.article6.en": "🇬🇧 English",
        "blog.article6.ru": "🇷🇺 Русский",
        "about.title": "О проекте",
        "about.developerFirst.title": "Developer-first подход",
        "about.developerFirst.p1": "Flagship создан разработчиками для разработчиков. Мы понимаем, что feature flags должны быть простыми в интеграции, типобезопасными и прозрачными.",
        "about.developerFirst.p2": "Проект находится в активной стадии разработки. Мы работаем над MVP и планируем запуск в ближайшие месяцы. Присоединяйтесь к разработке или следите за обновлениями!",
        "about.deployment.title": "Self-hosted или облако?",
        "about.deployment.selfhosted": "Self-hosted — для тех, кому нужен полный контроль над данными и инфраструктурой. Один Docker-compose файл, и всё работает.",
        "about.deployment.cloud": "Облако — для тех, кто хочет сфокусироваться на разработке, а не на поддержке инфраструктуры. Автоматическое масштабирование, мониторинг, обновления — всё из коробки.",
        "about.developer.title": "Разработчик",
        "about.developer.name": "Max Luxs",
        "about.developer.desc": "Разрабатываю Flagship в свободное время. Увлекаюсь Kotlin Multiplatform и чистой архитектурой.",
        "about.developer.more": "Открыт к обсуждениям, предложениям и контрибуциям. Проект создан из желания сделать feature flags простыми и доступными для всех.",
        "about.developer.contact": "Связаться",
        "status.title": "Проект в стадии активной разработки",
        "status.desc": "Мы работаем над MVP. Некоторые функции могут быть недоступны или изменяться. Следите за обновлениями в GitHub или присоединяйтесь к разработке!",
        "footer.resources": "Ресурсы",
        "footer.docs": "Документация",
        "footer.admin": "Админ-панель",
        "footer.support": "Поддержка",
        "footer.copyright": "© 2024 Flagship. Open Source SDK, коммерческий сервер.",
        "features.title": "Возможности",
        "deployment.title": "Варианты развертывания",
        "deployment.cloud.title": "Облако (SaaS)",
        "deployment.cloud.desc": "Готовое решение без настройки инфраструктуры. Просто подключите SDK и начните работу.",
        "deployment.cloud.feature1": "Автоматические обновления",
        "deployment.cloud.feature2": "Масштабирование из коробки",
        "deployment.cloud.feature3": "Мониторинг и аналитика",
        "deployment.cloud.feature4": "Техническая поддержка",
        "deployment.cloud.status": "Скоро",
        "deployment.selfhosted.title": "Self-hosted",
        "deployment.selfhosted.desc": "Полный контроль над данными. Развертывание на вашей инфраструктуре.",
        "deployment.selfhosted.feature1": "Docker-compose setup",
        "deployment.selfhosted.feature2": "PostgreSQL база данных",
        "deployment.selfhosted.feature3": "Админ-панель",
        "deployment.selfhosted.feature4": "REST API",
        "deployment.selfhosted.status": "Доступно",
        "deployment.selfhosted.docs": "Документация",
        "pricing.title": "Тарифы",
        "pricing.simple.title": "Цены в разработке",
        "pricing.simple.desc": "Проект находится в активной стадии разработки. Цены и тарифы будут определены позже.",
        "pricing.simple.open": "Мы открыты к предложениям по:",
        "pricing.simple.item1": "🤝 Сотрудничеству и партнёрству",
        "pricing.simple.item2": "📢 Продвижению и маркетингу",
        "pricing.simple.item3": "💻 Технической помощи и контрибуциям",
        "pricing.simple.item4": "💬 Обратной связи и предложениям",
        "pricing.simple.contact": "Свяжитесь с нами через GitHub или Issues, чтобы обсудить возможности сотрудничества!",
        "pricing.simple.button": "Связаться",
        "docs.title": "Документация",
        "docs.guide": "Руководство",
        "docs.guideDesc": "Полное руководство по использованию SDK",
        "docs.api": "API Reference",
        "docs.apiDesc": "Документация REST API",
        "docs.migration": "Migration Guide",
        "docs.migrationDesc": "Руководство по миграции",
        "docs.githubDesc": "Исходный код и issues",
        "blog.title": "Блог",
        "blog.subtitle": "Лучшие практики и статьи о feature flags, A/B тестировании и процессе разработки",
        "blog.article1.title": "Лучшие практики работы с Feature Flags",
        "blog.article1.desc": "Руководство по эффективному использованию feature flags в процессе разработки",
        "blog.article1.en": "🇬🇧 English",
        "blog.article1.ru": "🇷🇺 Русский",
        "blog.article2.title": "Процесс разработки с Feature Flags",
        "blog.article2.desc": "Как интегрировать feature flags в CI/CD и процесс разработки",
        "blog.article2.en": "🇬🇧 English",
        "blog.article2.ru": "🇷🇺 Русский",
        "blog.article3.title": "A/B тестирование с Feature Flags",
        "blog.article3.desc": "Проведение экспериментов и анализ результатов с помощью feature flags",
        "blog.article3.en": "🇬🇧 English",
        "blog.article3.ru": "🇷🇺 Русский",
        "blog.article4.title": "Безопасность и управление Feature Flags",
        "blog.article4.desc": "Практики безопасности, управление жизненным циклом и kill switches",
        "blog.article4.en": "🇬🇧 English",
        "blog.article4.ru": "🇷🇺 Русский",
        "blog.article5.title": "Мониторинг и аналитика Feature Flags",
        "blog.article5.desc": "Отслеживание использования флагов, метрики и дашборды",
        "blog.article5.en": "🇬🇧 English",
        "blog.article5.ru": "🇷🇺 Русский",
        "blog.article6.title": "Gradual Rollouts и Canary Releases",
        "blog.article6.desc": "Постепенное развертывание новых функций с минимальными рисками",
        "blog.article6.en": "🇬🇧 English",
        "blog.article6.ru": "🇷🇺 Русский",
        "about.title": "О проекте",
        "about.developerFirst.title": "Developer-first подход",
        "about.developerFirst.p1": "Flagship создан разработчиками для разработчиков. Мы понимаем, что feature flags должны быть простыми в интеграции, типобезопасными и прозрачными.",
        "about.developerFirst.p2": "Проект находится в активной стадии разработки. Мы работаем над MVP и планируем запуск в ближайшие месяцы. Присоединяйтесь к разработке или следите за обновлениями!",
        "about.deployment.title": "Self-hosted или облако?",
        "about.deployment.selfhosted": "Self-hosted — для тех, кому нужен полный контроль над данными и инфраструктурой. Один Docker-compose файл, и всё работает.",
        "about.deployment.cloud": "Облако — для тех, кто хочет сфокусироваться на разработке, а не на поддержке инфраструктуры. Автоматическое масштабирование, мониторинг, обновления — всё из коробки.",
        "about.developer.title": "Разработчик",
        "about.developer.name": "Max Luxs",
        "about.developer.desc": "Full-stack разработчик, Kotlin энтузиаст"
    },
    en: {
        title: "Flagship - Feature Flags & A/B Testing for Kotlin Multiplatform",
        "nav.features": "Features",
        "nav.pricing": "Pricing",
        "nav.docs": "Documentation",
        "nav.blog": "Blog",
        "nav.about": "About",
        "nav.admin": "Admin Panel",
        "hero.badge": "🚧 Under Active Development",
        "hero.title": "Feature Flags & A/B Testing for Kotlin Multiplatform",
        "hero.subtitle": "Simple integration, type safety, realtime updates. Self-hosted or cloud — your choice.",
        "hero.getStarted": "Get Started",
        "hero.stats.kmp": "Kotlin Multiplatform",
        "hero.stats.integration": "Integration",
        "hero.stats.opensource": "Open Source SDK",
        "features.title": "Features",
        "deployment.title": "Deployment Options",
        "deployment.cloud.title": "Cloud (SaaS)",
        "deployment.cloud.desc": "Ready-to-use solution without infrastructure setup. Just connect the SDK and start working.",
        "deployment.cloud.feature1": "Automatic updates",
        "deployment.cloud.feature2": "Scaling out of the box",
        "deployment.cloud.feature3": "Monitoring and analytics",
        "deployment.cloud.feature4": "Technical support",
        "deployment.cloud.status": "Coming Soon",
        "deployment.selfhosted.title": "Self-hosted",
        "deployment.selfhosted.desc": "Full control over your data. Deploy on your infrastructure.",
        "deployment.selfhosted.feature1": "Docker-compose setup",
        "deployment.selfhosted.feature2": "PostgreSQL database",
        "deployment.selfhosted.feature3": "Admin panel",
        "deployment.selfhosted.feature4": "REST API",
        "deployment.selfhosted.status": "Available",
        "deployment.selfhosted.docs": "Documentation",
        "pricing.title": "Pricing",
        "pricing.simple.title": "Pricing in Development",
        "pricing.simple.desc": "The project is in active development. Pricing and plans will be determined later.",
        "pricing.simple.open": "We're open to proposals for:",
        "pricing.simple.item1": "🤝 Partnership and collaboration",
        "pricing.simple.item2": "📢 Promotion and marketing",
        "pricing.simple.item3": "💻 Technical help and contributions",
        "pricing.simple.item4": "💬 Feedback and suggestions",
        "pricing.simple.contact": "Contact us via GitHub or Issues to discuss collaboration opportunities!",
        "pricing.simple.button": "Contact",
        "docs.title": "Documentation",
        "docs.guide": "Usage Guide",
        "docs.guideDesc": "Complete guide to using the SDK",
        "docs.api": "API Reference",
        "docs.apiDesc": "REST API documentation",
        "docs.migration": "Migration Guide",
        "docs.migrationDesc": "Migration guide",
        "docs.githubDesc": "Source code and issues",
        "blog.title": "Blog",
        "blog.subtitle": "Best practices and articles about feature flags, A/B testing, and development process",
        "blog.article1.title": "Feature Flags Best Practices",
        "blog.article1.desc": "Guide to effectively using feature flags in the development process",
        "blog.article1.en": "🇬🇧 English",
        "blog.article1.ru": "🇷🇺 Русский",
        "blog.article2.title": "Development Process with Feature Flags",
        "blog.article2.desc": "How to integrate feature flags into CI/CD and development workflow",
        "blog.article2.en": "🇬🇧 English",
        "blog.article2.ru": "🇷🇺 Русский",
        "blog.article3.title": "A/B Testing with Feature Flags",
        "blog.article3.desc": "Running experiments and analyzing results using feature flags",
        "blog.article3.en": "🇬🇧 English",
        "blog.article3.ru": "🇷🇺 Русский",
        "blog.article4.title": "Feature Flags Security and Management",
        "blog.article4.desc": "Security practices, lifecycle management, and kill switches",
        "blog.article4.en": "🇬🇧 English",
        "blog.article4.ru": "🇷🇺 Русский",
        "blog.article5.title": "Feature Flags Monitoring and Analytics",
        "blog.article5.desc": "Tracking flag usage, metrics, and dashboards",
        "blog.article5.en": "🇬🇧 English",
        "blog.article5.ru": "🇷🇺 Русский",
        "blog.article6.title": "Gradual Rollouts and Canary Releases",
        "blog.article6.desc": "Gradually deploying new features with minimal risk",
        "blog.article6.en": "🇬🇧 English",
        "blog.article6.ru": "🇷🇺 Русский",
        "about.title": "About",
        "about.developerFirst.title": "Developer-first approach",
        "about.developerFirst.p1": "Flagship is built by developers for developers. We understand that feature flags should be simple to integrate, type-safe, and transparent.",
        "about.developerFirst.p2": "The project is in active development. We're working on MVP and planning to launch in the coming months. Join the development or follow updates!",
        "about.deployment.title": "Self-hosted or cloud?",
        "about.deployment.selfhosted": "Self-hosted — for those who need full control over data and infrastructure. One Docker-compose file, and everything works.",
        "about.deployment.cloud": "Cloud — for those who want to focus on development, not infrastructure maintenance. Automatic scaling, monitoring, updates — everything out of the box.",
        "about.developer.title": "Developer",
        "about.developer.name": "Max Luxs",
        "about.developer.desc": "Developing Flagship in my spare time. Passionate about Kotlin Multiplatform and clean architecture.",
        "about.developer.more": "Open to discussions, suggestions, and contributions. The project was created from a desire to make feature flags simple and accessible to everyone.",
        "about.developer.contact": "Contact",
        "status.title": "Project Under Active Development",
        "status.desc": "We're working on MVP. Some features may be unavailable or change. Follow updates on GitHub or join the development!",
        "footer.resources": "Resources",
        "footer.docs": "Documentation",
        "footer.admin": "Admin Panel",
        "footer.support": "Support",
        "footer.copyright": "© 2024 Flagship. Open Source SDK, commercial server."
    }
};

let currentLanguage = localStorage.getItem('language') || 'ru';

function setLanguage(lang) {
    currentLanguage = lang;
    localStorage.setItem('language', lang);
    document.getElementById('html-root').setAttribute('lang', lang);
    document.getElementById('language-selector').value = lang;
    
    // Update all elements with data-i18n attribute
    document.querySelectorAll('[data-i18n]').forEach(element => {
        const key = element.getAttribute('data-i18n');
        if (translations[lang] && translations[lang][key]) {
            if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
                element.placeholder = translations[lang][key];
            } else if (element.tagName === 'TITLE') {
                element.textContent = translations[lang][key];
            } else {
                // For elements with nested tags (like <strong>), use innerHTML
                // But be careful - only if there are nested tags
                const hasNestedTags = element.querySelector('strong, em, span, a');
                if (hasNestedTags) {
                    // Preserve structure but replace text
                    const text = translations[lang][key];
                    // Simple replacement - preserve <strong> tags if they exist
                    if (text.includes('Self-hosted') || text.includes('Облако')) {
                        element.innerHTML = text.replace(/Self-hosted|Облако/g, (match) => {
                            return `<strong>${match}</strong>`;
                        });
                    } else {
                        element.innerHTML = text;
                    }
                } else {
                    element.textContent = translations[lang][key];
                }
            }
        }
    });
    
    // Update title
    if (translations[lang] && translations[lang].title) {
        document.title = translations[lang].title;
    }
}

// Initialize language on page load
document.addEventListener('DOMContentLoaded', () => {
    setLanguage(currentLanguage);
    
    // Language selector change handler
    document.getElementById('language-selector').addEventListener('change', (e) => {
        setLanguage(e.target.value);
    });
});

