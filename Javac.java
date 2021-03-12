import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Javac {
	static File tmp = new File(System.getProperty("java.io.tmpdir"));
	static String head = "import java.io.*;\n" + "import java.util.*;\n" + "public class Src {\n" + "\tpublic static void main(String[] args) {", foot = "\t}\n" + "}\n";
	public static void main(String[] args) throws IOException, InterruptedException {
		StringBuilder code = new StringBuilder(head);
		code.append(String.join(" ", args));
		code.append(foot);
		
		File temp = new File(tmp, "Src.java");
		
		temp.delete();
		Files.writeString(temp.toPath(), code, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		temp.deleteOnExit();
		boolean javac = false;
		try{
			javac = run("javac", "--version") == 0;
			
		}catch (IOException ignored){}
		System.out.println("javac: " + (javac ? "Exists" : "Nope"));
		if(!javac){
			System.out.println("Don't have javac");
			return;
		}
		System.out.println("javac: " + run("javac", "Src.java"));
		System.out.println("Executing");
		System.out.println("java: " + run("java", "Src"));
	}
	
	public static int run(String... arg) throws IOException, InterruptedException {
		return new ProcessBuilder(arg).directory(tmp).inheritIO().start().waitFor();
	}
}
