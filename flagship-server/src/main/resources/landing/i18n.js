// Internationalization for Flagship Landing Page

const translations = {
    ru: {
        title: "Flagship - Feature Flags & A/B Testing для Kotlin Multiplatform",
        "nav.features": "Возможности",
        "nav.pricing": "Тарифы",
        "nav.docs": "Документация",
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
        "pricing.popular": "Популярный",
        "pricing.free.title": "Free",
        "pricing.free.feature1": "До 5 флагов",
        "pricing.free.feature2": "1 проект",
        "pricing.free.feature3": "10k MAU",
        "pricing.free.feature4": "REST API",
        "pricing.free.feature5": "Базовые SDK",
        "pricing.free.feature6": "Локальный кэш",
        "pricing.free.button": "Начать бесплатно",
        "pricing.pro.title": "Pro",
        "pricing.pro.feature1": "До 50 флагов",
        "pricing.pro.feature2": "5 проектов",
        "pricing.pro.feature3": "500k MAU",
        "pricing.pro.feature4": "Realtime обновления",
        "pricing.pro.feature5": "Typed flags",
        "pricing.pro.feature6": "Базовая аналитика",
        "pricing.pro.button": "Начать",
        "pricing.growth.title": "Growth",
        "pricing.growth.feature1": "До 200 флагов",
        "pricing.growth.feature2": "20 проектов",
        "pricing.growth.feature3": "2M MAU",
        "pricing.growth.feature4": "Расширенная аналитика",
        "pricing.growth.feature5": "Интеграции",
        "pricing.growth.feature6": "Audit log",
        "pricing.growth.button": "Связаться",
        "pricing.enterprise.title": "Enterprise",
        "pricing.enterprise.feature1": "Без ограничений",
        "pricing.enterprise.feature2": "SLA",
        "pricing.enterprise.feature3": "ML алгоритмы",
        "pricing.enterprise.feature4": "Edge delivery",
        "pricing.enterprise.feature5": "Compliance",
        "pricing.enterprise.feature6": "Приоритетная поддержка",
        "pricing.enterprise.button": "Связаться",
        "pricing.note": "💡 Self-hosted доступен для всех тарифов. Лицензия + поддержка.",
        "docs.title": "Документация",
        "docs.guide": "Руководство",
        "docs.guideDesc": "Полное руководство по использованию SDK",
        "docs.api": "API Reference",
        "docs.apiDesc": "Документация REST API",
        "docs.migration": "Migration Guide",
        "docs.migrationDesc": "Руководство по миграции",
        "docs.githubDesc": "Исходный код и issues",
        "about.title": "О проекте",
        "about.developerFirst.title": "Developer-first подход",
        "about.developerFirst.p1": "Flagship создан разработчиками для разработчиков. Мы понимаем, что feature flags должны быть простыми в интеграции, типобезопасными и прозрачными.",
        "about.developerFirst.p2": "Проект находится в активной стадии разработки. Мы работаем над MVP и планируем запуск в ближайшие месяцы. Присоединяйтесь к разработке или следите за обновлениями!",
        "about.deployment.title": "Self-hosted или облако?",
        "about.deployment.selfhosted": "Self-hosted — для тех, кому нужен полный контроль над данными и инфраструктурой. Один Docker-compose файл, и всё работает.",
        "about.deployment.cloud": "Облако — для тех, кто хочет сфокусироваться на разработке, а не на поддержке инфраструктуры. Автоматическое масштабирование, мониторинг, обновления — всё из коробки.",
        "about.developer.title": "Разработчик",
        "about.developer.name": "Max Luxs",
        "about.developer.desc": "Full-stack разработчик, Kotlin энтузиаст",
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
        "pricing.popular": "Популярный",
        "pricing.free.title": "Free",
        "pricing.free.feature1": "До 5 флагов",
        "pricing.free.feature2": "1 проект",
        "pricing.free.feature3": "10k MAU",
        "pricing.free.feature4": "REST API",
        "pricing.free.feature5": "Базовые SDK",
        "pricing.free.feature6": "Локальный кэш",
        "pricing.free.button": "Начать бесплатно",
        "pricing.pro.title": "Pro",
        "pricing.pro.feature1": "До 50 флагов",
        "pricing.pro.feature2": "5 проектов",
        "pricing.pro.feature3": "500k MAU",
        "pricing.pro.feature4": "Realtime обновления",
        "pricing.pro.feature5": "Typed flags",
        "pricing.pro.feature6": "Базовая аналитика",
        "pricing.pro.button": "Начать",
        "pricing.growth.title": "Growth",
        "pricing.growth.feature1": "До 200 флагов",
        "pricing.growth.feature2": "20 проектов",
        "pricing.growth.feature3": "2M MAU",
        "pricing.growth.feature4": "Расширенная аналитика",
        "pricing.growth.feature5": "Интеграции",
        "pricing.growth.feature6": "Audit log",
        "pricing.growth.button": "Связаться",
        "pricing.enterprise.title": "Enterprise",
        "pricing.enterprise.feature1": "Без ограничений",
        "pricing.enterprise.feature2": "SLA",
        "pricing.enterprise.feature3": "ML алгоритмы",
        "pricing.enterprise.feature4": "Edge delivery",
        "pricing.enterprise.feature5": "Compliance",
        "pricing.enterprise.feature6": "Приоритетная поддержка",
        "pricing.enterprise.button": "Связаться",
        "pricing.note": "💡 Self-hosted доступен для всех тарифов. Лицензия + поддержка.",
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
        "pricing.popular": "Popular",
        "pricing.free.title": "Free",
        "pricing.free.feature1": "Up to 5 flags",
        "pricing.free.feature2": "1 project",
        "pricing.free.feature3": "10k MAU",
        "pricing.free.feature4": "REST API",
        "pricing.free.feature5": "Basic SDKs",
        "pricing.free.feature6": "Local cache",
        "pricing.free.button": "Get Started Free",
        "pricing.pro.title": "Pro",
        "pricing.pro.feature1": "Up to 50 flags",
        "pricing.pro.feature2": "5 projects",
        "pricing.pro.feature3": "500k MAU",
        "pricing.pro.feature4": "Realtime updates",
        "pricing.pro.feature5": "Typed flags",
        "pricing.pro.feature6": "Basic analytics",
        "pricing.pro.button": "Get Started",
        "pricing.growth.title": "Growth",
        "pricing.growth.feature1": "Up to 200 flags",
        "pricing.growth.feature2": "20 projects",
        "pricing.growth.feature3": "2M MAU",
        "pricing.growth.feature4": "Advanced analytics",
        "pricing.growth.feature5": "Integrations",
        "pricing.growth.feature6": "Audit log",
        "pricing.growth.button": "Contact",
        "pricing.enterprise.title": "Enterprise",
        "pricing.enterprise.feature1": "Unlimited",
        "pricing.enterprise.feature2": "SLA",
        "pricing.enterprise.feature3": "ML algorithms",
        "pricing.enterprise.feature4": "Edge delivery",
        "pricing.enterprise.feature5": "Compliance",
        "pricing.enterprise.feature6": "Priority support",
        "pricing.enterprise.button": "Contact",
        "pricing.note": "💡 Self-hosted available for all plans. License + support.",
        "docs.title": "Documentation",
        "docs.guide": "Usage Guide",
        "docs.guideDesc": "Complete guide to using the SDK",
        "docs.api": "API Reference",
        "docs.apiDesc": "REST API documentation",
        "docs.migration": "Migration Guide",
        "docs.migrationDesc": "Migration guide",
        "docs.githubDesc": "Source code and issues",
        "about.title": "About",
        "about.developerFirst.title": "Developer-first approach",
        "about.developerFirst.p1": "Flagship is built by developers for developers. We understand that feature flags should be simple to integrate, type-safe, and transparent.",
        "about.developerFirst.p2": "The project is in active development. We're working on MVP and planning to launch in the coming months. Join the development or follow updates!",
        "about.deployment.title": "Self-hosted or cloud?",
        "about.deployment.selfhosted": "Self-hosted — for those who need full control over data and infrastructure. One Docker-compose file, and everything works.",
        "about.deployment.cloud": "Cloud — for those who want to focus on development, not infrastructure maintenance. Automatic scaling, monitoring, updates — everything out of the box.",
        "about.developer.title": "Developer",
        "about.developer.name": "Max Luxs",
        "about.developer.desc": "Full-stack developer, Kotlin enthusiast",
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

