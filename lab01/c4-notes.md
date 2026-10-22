# C4 Architectural Notes: Monthly Sales Report System

## 1. C4 Level 1: System Context Diagram (`c4-context.mmd`)

- **What the diagram deliberately does not show:**  
  The context diagram deliberately omits internal software containers, databases, internal pipelines, execution threading models, and server infrastructure, focusing exclusively on external actors and system boundaries.
- **Quality-Attribute Scenario Served:**  
  The system boundary serves **QA-5 (Security)** by defining the perimeter for role-based authorization separating head-office analysts from branch managers, and **QA-3 (Availability)** by decoupling external POS systems via scheduled SFTP file uploads.
- **Open Question for the Client:**  
  Does the Finance Director need an attached PDF copy of the monthly report delivered with the email notification, or is an automated email status alert with a web link sufficient?

---

## 2. C4 Level 2: Container Diagram (`c4-container.mmd`)

- **What the diagram deliberately does not show:**  
  The container diagram deliberately omits internal class hierarchies, domain models (`SaleTransaction`, `MonthlyReport`), package structures (`edu.itc.salesreport.model`, `ingest`, `render`), and fine-grained algorithms within each runnable unit.
- **Quality-Attribute Scenarios Served by Containers:**  
  - **Report Batch (`engine`):** Directly serves **QA-1 (Performance)** by executing batch in-memory aggregation of ~3 million rows in $\le 60\text{ s}$ and **QA-2 (Scalability)** under a constrained 512 MB heap.
  - **Ingest Job (`ingest`):** Serves **QA-3 (Availability)** by validating daily POS feeds independently with retry logic, isolating broken branch WAN links to produce partial reports without crashing the pipeline.
  - **Web Report Page (`web`):** Serves **QA-5 (Security)** by enforcing role-based access control filters that restrict branch managers strictly to their own branch's sales metrics.
- **Open Question for the Client:**  
  Will the Landing Zone and Report Store share a clustered network file system (NFS/SMB) across environments, or will all containers in Release 1 execute on a single dedicated server using local NVMe storage?
