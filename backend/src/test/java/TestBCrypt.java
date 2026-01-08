import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate hash for test123
        String test123Hash = encoder.encode("test123");
        System.out.println("test123 hash: " + test123Hash);
        System.out.println("Verifies with test123: " + encoder.matches("test123", test123Hash));

        // Generate hash for admin123
        String admin123Hash = encoder.encode("admin123");
        System.out.println("\nadmin123 hash: " + admin123Hash);
        System.out.println("Verifies with admin123: " + encoder.matches("admin123", admin123Hash));
    }
}
