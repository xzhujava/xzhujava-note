import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * hashMap
 * @author xzhu
 */
public class Hash{
    public static void main(String[] args) {
        Map<String,String> map = new HashMap<>();
        Map<String,String> threaSafeMap = new ConcurrentHashMap<>();
    }
}