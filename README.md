# Cloud-Based Project Management System

A cloud-based project management application designed for small software development teams to organize projects, assign tasks, collaborate, and track progress through a simple Kanban workflow.

> **Project status:** In development. The UI/UX design phase is complete, and development and DevOps setup are the next steps. Features, integrations, and infrastructure described below are planned unless explicitly marked as completed.

## Why I am building this project

I am developing this application **for learning purposes** to gain practical experience with the complete Software Development Life Cycle (SDLC), Agile/Scrum practices, full-stack development, AWS cloud services, and DevOps.

Rather than focusing only on writing application code, my goal is to learn how a software product is planned, designed, developed, tested, deployed, monitored, and maintained. I also want to build a practical CI/CD pipeline using GitHub Actions and AWS while exploring cloud security, infrastructure automation, containerization, and cost optimization.

This is a portfolio and educational project, not a commercial product.

## Planned features

- **Spaces:** Create and manage team workspaces and their members.
- **Projects:** Create projects, appoint project managers, and assign members from a Space.
- **Tasks:** Create, assign, prioritize, and track tasks.
- **Kanban board:** Manage tasks across **To Do**, **In Progress**, and **Completed**.
- **Comments:** Collaborate through task-level discussions.
- **Dashboard:** View project progress, remaining tasks, and recently completed tasks.
- **My Tasks:** View and filter tasks assigned to the signed-in user.
- **Member management:** Invite members, manage membership, and transfer Space ownership.
- **Account management:** Update profile picture, name, and email address.
- **Authentication:** Registration, login, OTP verification, and password reset.
- **Notifications:** Planned support for task assignments, updates, and reminders.

### Application hierarchy

```text
Space
  └── Project
        └── Task
              └── Comment
```

### Roles

| Role | Planned responsibilities |
| --- | --- |
| Space Owner | Manage the Space, its members and projects; appoint project managers; transfer ownership |
| Space Member | Access the Space and participate in permitted projects |
| Project Manager | Manage assigned projects, select project members, create and assign tasks |
| Project Member | View relevant project work, update permitted task details, and comment |

Space Owner and Space Member are Space-level roles. Project Manager and Project Member are project-level roles.

## Technology stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Frontend | Next.js, React, TypeScript | Web application and user interface |
| Backend | Java, Spring Boot | REST APIs and business logic |
| Database | PostgreSQL | Relational application data |
| Containerization | Docker | Package and run application services |
| Version control | Git, GitHub | Source control, collaboration, and pull requests |
| CI/CD | GitHub Actions | Automated validation, builds, and deployments |
| Cloud platform | Amazon Web Services (AWS) | Hosting, storage, security, events, and operations |
| Infrastructure as Code | AWS CloudFormation | Define and manage cloud infrastructure |
| Development approach | Agile/Scrum, Kanban | Iterative planning and delivery |
| UI/UX design | Google Stitch, Figma, Penpot | Design and prototype interfaces |

The project is planned as a **monorepo**, with independently built frontend and backend applications.

## Planned AWS services

The services below are part of the learning and implementation roadmap. Not all services will be used in the initial release.

### Core infrastructure and deployment

| AWS service | Intended use |
| --- | --- |
| **IAM** | Least-privilege access, deployment roles, and GitHub Actions OIDC authentication |
| **VPC** | Networking, subnets, routing, and security groups |
| **EC2** | Host the containerized Spring Boot backend |
| **EBS** | Block storage for EC2 |
| **ECR** | Store Docker container images |
| **S3** | Store profile pictures and application file uploads; potentially host static frontend assets |
| **CloudFront** | Deliver static frontend assets when appropriate for the selected Next.js deployment mode |
| **RDS for PostgreSQL** | Evaluate managed relational database hosting, subject to budget |
| **Cognito** | Evaluate managed authentication and account verification |
| **CloudFormation** | Provision and version infrastructure |
| **CloudWatch** | Application logs, metrics, monitoring, and alarms |
| **CloudTrail** | Audit AWS account and API activity |
| **AWS Budgets / Cost Explorer** | Spending alerts, usage visibility, and cost optimization |

The final frontend hosting method will be selected based on whether the Next.js application requires server-side rendering. PostgreSQL may initially run in a local or lower-cost environment before evaluating RDS.

### Application events and asynchronous processing

| AWS service | Intended use |
| --- | --- |
| **SQS** | Queue background processing jobs |
| **Lambda** | Process asynchronous events and lightweight background tasks |
| **EventBridge** | Route application events and support task reminders |
| **SNS** | Explore notifications and event fan-out |
| **DynamoDB** | Optional experiment for activity or event records |

### Optional AWS learning experiments

| AWS service | Learning objective |
| --- | --- |
| **ECS / Fargate** | Compare managed container deployment with EC2 |
| **Elastic Beanstalk** | Explore managed application deployment |
| **Elastic Load Balancing** | Understand load balancing and health checks |
| **EC2 Auto Scaling** | Explore scaling and availability |
| **Systems Manager Parameter Store / Secrets Manager** | Secure configuration and secrets management |
| **Amazon Bedrock** | Optional experiment with AI-generated project summaries |

Costly or complex services will be evaluated through short, controlled experiments rather than kept running unnecessarily.

## Planned CI/CD workflow

GitHub Actions will be used for automated checks and deployments. AWS access from GitHub will use **OpenID Connect (OIDC)** and an IAM role instead of permanent AWS access keys.

```text
Developer push / pull request
            |
            v
      GitHub Actions
            |
            +--> Frontend: lint, test, build
            |
            +--> Backend: test, build
            |
            v
    Merge to main (after checks)
            |
            v
      Deployment workflow
            |
            v
      Assume AWS IAM role
            |
            v
    Docker build and push to ECR
            |
            v
      Deploy backend to EC2
            |
            v
      Health checks and logs
            |
            v
          CloudWatch
```

Frontend and backend workflows will be separated using path-based triggers so changes to one application do not always rebuild the other. The deployment pipeline will be implemented incrementally and extended with health checks and rollback procedures.

## Proposed repository structure

```text
cloud-project-management-system/
├── frontend/                 # Next.js + TypeScript
├── backend/                  # Spring Boot + Java
├── infrastructure/           # AWS CloudFormation templates
├── docs/                     # SRS, architecture, and SDLC documentation
├── .github/
│   └── workflows/            # CI/CD workflows
└── README.md
```

This is the intended structure; directories will be added as development progresses.

## Development roadmap

- [x] Define initial requirements and MVP scope 
- [x] Complete the initial UI/UX design phase
- [ ] Initialize the monorepo and application projects
- [ ] Configure GitHub Actions CI checks
- [x] Configure AWS IAM, GitHub OIDC, and cost alerts
- [ ] Develop authentication and role-based access
- [ ] Implement Spaces, projects, members, and tasks
- [ ] Implement Kanban, comments, and dashboard
- [ ] Containerize and deploy the backend
- [ ] Deploy the frontend and configure HTTPS
- [ ] Integrate AWS storage, monitoring, and event-driven features
- [ ] Manage infrastructure through CloudFormation
- [ ] Perform end-to-end testing, security review, and cost optimization
- [ ] Explore optional AWS deployment and scaling experiments

## Cost awareness

This project is being developed with a limited AWS learning budget. The current **planning ceiling is $40** for this project, with a preference to spend less. The budget is a target for cost management, **not an automatic AWS spending cap**.

To reduce unnecessary charges, I plan to use local development where practical, set AWS Budgets alerts, monitor usage, and shut down or remove temporary experimental resources after testing.

## Project purpose and disclaimer

This repository documents my hands-on learning journey across full-stack development, cloud computing, CI/CD, infrastructure automation, security, testing, monitoring, and Agile delivery.

The architecture and service choices may evolve as I learn, evaluate costs, and implement the system. Items described as planned should not be interpreted as already deployed or completed.
