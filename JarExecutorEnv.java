import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

//javac Main.java
//java Main https://your_url_to_jar_file.jar Main.EntryPoint --Argument here
//Tested with java 11
public class JarExecutorEnv {
    public static JarLoader lib;
    
    public static void main(String[] args) throws Throwable {
        args = System.getenv("args").split(":");
        String main = System.getenv("main");
        System.out.println("Hacking time");
        lib = new JarLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
        lib.addURL(System.getenv("urls").split(":"));
        Class<?> mainClass = lib.loadClass(main);
        Method m = mainClass.getMethod("main", String[].class);
        System.out.println("Initializing main method\nArguments:");
        System.out.println(Arrays.toString(args));
        m.invoke(null, (Object) args);//cast it to object because java is confused
        
    }
    
    public static class JarLoader extends URLClassLoader {
        static {
            registerAsParallelCapable();
        }
        
        public JarLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        public JarLoader(URL[] urls) {
            super(urls);
        }
        
        
        public void addURL(String... urls) throws MalformedURLException {
            for (String s : urls)
                addURL(new URL(s));
        }
        
        @Override
        public void addURL(URL jar) {
            
            try {
                
                File temp = new File(System.getProperty("java.io.tmpdir"), jar.getFile());
                if (!temp.getParentFile().exists())
                    System.out.println(temp.getAbsolutePath() + ": " + temp.getParentFile().mkdirs());
                if (!temp.exists()){
                    System.out.println("Downloading: " + jar);
                    FileOutputStream o = new FileOutputStream(temp);
                    o.write(jar.openStream().readAllBytes());
                    o.close();
                }
                jar = temp.toURI().toURL();
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("Adding: " + jar);
            super.addURL(jar);
        }
        
        
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }
}
