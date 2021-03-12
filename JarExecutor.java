import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
//javac Main.java
//java Main https://your_url_to_jar_file.jar Main.EntryPoint --Argument here
//Tested with java 11
public class JarExecutor {
    public static JarLoader lib;
  
    public static void main(String[] args) throws Throwable{
        System.out.println("Hacking time");
        URL jar = new URL(args[0]);
        File temp = new File(System.getProperty("java.io.tmpdir"), jar.getFile());
        if(!temp.exists()) {
            FileOutputStream o = new FileOutputStream(temp);
            o.write(jar.openStream().readAllBytes());
            o.close();
        }
        
        lib = new JarLoader(new URL[]{temp.toURI().toURL()}, ClassLoader.getSystemClassLoader());
        System.out.println("Downloading: " + args[0]);
        Class<?> mainClass = lib.loadClass(args[1]);
        Method m = mainClass.getMethod("main", String[].class);
        //behold shitty way to pass args
        args[0] = "";
        args[1] = "";
        System.out.println("Parsing arguments");
        String[] arg = new String[Math.toIntExact(Arrays.stream(args).filter(s -> !s.isEmpty()).count())];
        int o = 0;
        for(String e : args){
            if(e.isEmpty())continue;
            arg[o] = e;
            o++;
        }
        System.out.println("Initializing main method\nArguments:");
        System.out.println(Arrays.toString(arg));
        m.invoke(null, (Object) arg);//cast it to object because java is confused

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


        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }


        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }
}
