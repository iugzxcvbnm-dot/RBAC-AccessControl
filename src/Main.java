import java.util.*;

/**
 * Role-Based Access Control (RBAC) demo - Java
 * Object: "Account"   Roles: Admin, Manager, Customer
 * Users are assigned to roles; permissions are assigned to roles (never directly to users).
 * Run:  java Main.java          -> interactive menu
 *       java Main.java --demo   -> automatic demonstration
 */
public class Main {

    // ---------------------------------------------------------- RBAC core
    static class RBAC {
        Map<String, Set<String>> rolePermissions = new LinkedHashMap<>(); // role -> permissions
        Map<String, Set<String>> userRoles = new LinkedHashMap<>();       // user -> roles

        void addRole(String role) {
            rolePermissions.putIfAbsent(role, new TreeSet<>());
        }

        void assignPermission(String role, String permission) {          // PA
            if (!rolePermissions.containsKey(role))
                throw new IllegalArgumentException("Role '" + role + "' does not exist");
            rolePermissions.get(role).add(permission);
        }

        void assignUser(String user, String role) {                      // UA
            if (!rolePermissions.containsKey(role))
                throw new IllegalArgumentException("Role '" + role + "' does not exist");
            userRoles.computeIfAbsent(user, k -> new TreeSet<>()).add(role);
        }

        boolean isAllowed(String user, String permission) {
            for (String role : userRoles.getOrDefault(user, Set.of()))
                if (rolePermissions.get(role).contains(permission)) return true;
            return false;
        }
    }

    // ------------------------------------------------- protected resource
    static class AccountService {
        RBAC rbac;
        Map<String, String[]> accounts = new HashMap<>();   // id -> {owner, balance}

        AccountService(RBAC rbac) { this.rbac = rbac; }

        boolean check(String user, String perm) {
            if (!rbac.isAllowed(user, perm)) {
                System.out.println("  [DENIED ] " + user + " is not allowed to " + perm + " accounts");
                return false;
            }
            System.out.println("  [GRANTED] " + user + " -> " + perm + " account");
            return true;
        }

        String show(String id) {
            String[] a = accounts.get(id);
            return a == null ? "not found" : "{owner=" + a[0] + ", balance=" + a[1] + "}";
        }

        void create(String user, String id, String owner, String balance) {
            if (!check(user, "Create")) return;
            if (accounts.containsKey(id)) { System.out.println("  Account already exists"); return; }
            accounts.put(id, new String[]{owner, balance});
            System.out.println("  Account " + id + " created: " + show(id));
        }

        void view(String user, String id) {
            if (check(user, "View")) System.out.println("  Account " + id + ": " + show(id));
        }

        void update(String user, String id, String balance) {
            if (!check(user, "Update")) return;
            if (accounts.containsKey(id)) {
                accounts.get(id)[1] = balance;
                System.out.println("  Account " + id + " updated: " + show(id));
            } else System.out.println("  Account not found");
        }

        void delete(String user, String id) {
            if (!check(user, "Delete")) return;
            if (accounts.remove(id) != null) System.out.println("  Account " + id + " deleted");
            else System.out.println("  Account not found");
        }
    }

    // ------------------------------------------------------------- setup
    static RBAC rbac;
    static AccountService svc;

    static void buildSystem() {
        rbac = new RBAC();
        for (String r : new String[]{"Admin", "Manager", "Customer"}) rbac.addRole(r);
        for (String p : new String[]{"Create", "View", "Update", "Delete"}) rbac.assignPermission("Admin", p);
        rbac.assignPermission("Manager", "View");
        rbac.assignPermission("Manager", "Update");
        rbac.assignPermission("Customer", "View");
        rbac.assignUser("ahmad", "Admin");
        rbac.assignUser("sara", "Manager");
        rbac.assignUser("layla", "Customer");
        svc = new AccountService(rbac);
    }

    static void showTables() {
        System.out.println("Role -> Permissions");
        rbac.rolePermissions.forEach((r, p) ->
                System.out.printf("  %-9s: %s%n", r, String.join(", ", p)));
        System.out.println("User -> Roles");
        rbac.userRoles.forEach((u, r) ->
                System.out.printf("  %-9s: %s%n", u, String.join(", ", r)));
    }

    // -------------------------------------------------------------- demo
    static void demo() {
        buildSystem();
        showTables();
        System.out.println("\n--- Admin (ahmad) ---");
        svc.create("ahmad", "A1", "Ali", "1000");
        svc.view("ahmad", "A1");
        svc.update("ahmad", "A1", "1500");
        System.out.println("\n--- Manager (sara) ---");
        svc.create("sara", "A2", "Khaled", "500");
        svc.view("sara", "A1");
        svc.update("sara", "A1", "2000");
        svc.delete("sara", "A1");
        System.out.println("\n--- Customer (layla) ---");
        svc.view("layla", "A1");
        svc.update("layla", "A1", "99999");
        svc.delete("layla", "A1");
        System.out.println("\n--- Assign a new user to a role at run time ---");
        rbac.assignUser("omar", "Manager");
        svc.update("omar", "A1", "2500");
        svc.delete("omar", "A1");
        System.out.println("\n--- Admin deletes the account ---");
        svc.delete("ahmad", "A1");
        svc.view("ahmad", "A1");
    }

    // ------------------------------------------------------- interactive
    static void menu() {
        buildSystem();
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\n===== RBAC Menu =====");
            System.out.println("1) Show roles & users   2) Assign user to role");
            System.out.println("3) Add permission to role   4) Create   5) View");
            System.out.println("6) Update   7) Delete   0) Exit");
            System.out.print("Choice: ");
            if (!in.hasNextLine()) break;
            String c = in.nextLine().trim();
            try {
                switch (c) {
                    case "0": return;
                    case "1": showTables(); break;
                    case "2": rbac.assignUser(ask(in, "User: "), ask(in, "Role: ")); break;
                    case "3": rbac.assignPermission(ask(in, "Role: "), ask(in, "Permission: ")); break;
                    case "4": case "5": case "6": case "7":
                        String u = ask(in, "Acting user: ");
                        String id = ask(in, "Account id: ");
                        if (c.equals("4")) svc.create(u, id, ask(in, "Owner: "), ask(in, "Balance: "));
                        else if (c.equals("5")) svc.view(u, id);
                        else if (c.equals("6")) svc.update(u, id, ask(in, "New balance: "));
                        else svc.delete(u, id);
                        break;
                    default: System.out.println("  Invalid choice");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
    }

    static String ask(Scanner in, String p) {
        System.out.print(p);
        return in.nextLine().trim();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--demo")) demo(); else menu();
    }
}
