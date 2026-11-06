-- Run ONCE in SQL Server Management Studio (connected with Windows Authentication, as an administrator).
-- Run the WHOLE script (do not select only some lines) with Execute / F5.
-- Creates the database "secc" and the SQL login "secc" used by the Spring Boot backend.
-- The tables are created automatically by Flyway when the backend starts for the first time.

USE master;
GO

PRINT 'Connected as: ' + SUSER_SNAME() + '  |  sysadmin = ' + CAST(IS_SRVROLEMEMBER('sysadmin') AS VARCHAR(5));
GO

-- 1. database
IF DB_ID(N'secc') IS NULL
BEGIN
    BEGIN TRY
        EXEC (N'CREATE DATABASE secc');
        PRINT 'Database secc created.';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR creating database secc: ' + ERROR_MESSAGE();
        THROW;
    END CATCH
END
ELSE
    PRINT 'Database secc already exists.';
GO

-- 2. login (server level)
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'secc')
BEGIN
    CREATE LOGIN secc WITH PASSWORD = N'Secc_Pass2026!', DEFAULT_DATABASE = master, CHECK_POLICY = OFF;
    PRINT 'Login secc created.';
END
ELSE
    PRINT 'Login secc already exists.';
GO

-- 3. user inside the database (dynamic SQL, so this works even if the database was just created)
EXEC (N'USE secc;
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N''secc'')
    CREATE USER secc FOR LOGIN secc;
ALTER ROLE db_owner ADD MEMBER secc;
PRINT ''User secc is db_owner of database secc.'';');
GO

ALTER LOGIN secc WITH DEFAULT_DATABASE = secc;
GO

-- 4. authentication mode: 1 = Windows only (SQL logins NOT allowed), 0 = Mixed mode (OK)
SELECT SERVERPROPERTY('IsIntegratedSecurityOnly') AS windows_auth_only;
