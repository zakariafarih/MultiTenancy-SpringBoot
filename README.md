# MultiTenancy-SpringBoot

A plug-and-play Java library for multitenancy in Spring Boot SaaS platforms, supporting:
- One PostgreSQL database per tenant
- One schema per microservice
- Dynamic datasource routing
- Flyway migration per tenant + schema
- Zero boilerplate repository access

> Ideal for building high-scale, isolated, and production-ready B2B SaaS platforms (e.g., Dental, EdTech, FinTech, CRM, etc.)

## Structure

📦 multitenancy-lib  
├── multitenancy-core – Tenant context, bootstrap, schema creation  
├── multitenancy-routing – Routing  &   
├── multitenancy-repo – Runtime JPA repository generation per tenant/schema  
├── multitenancy-autoconfig – Spring Boot AutoConfiguration  
└── multitenancy-examples – Sample clinic-core microservice using the library

## Getting Started

Coming soon…

