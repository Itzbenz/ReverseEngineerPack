import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Javac {
	
	static String head = "import java.io.*;\n" + "import java.util.*;\n" + "public class Src {\n" + "\tpublic static void main(String[] args) {", foot = "\t}\n" + "}\n";
	public static void main(String[] args) throws IOException, InterruptedException {
		StringBuilder code = new StringBuilder(head);
		code.append(String.join(" ", args));
		code.append(foot);
		File temp = new File(System.getProperty("java.io.tmpdir"), "Src.java");
		temp.delete();
		Files.writeString(temp.toPath(), code, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		temp.deleteOnExit();
		boolean javac = false;
		try{
			javac = Runtime.getRuntime().exec("javac --version").waitFor() == 0;
		}catch (IOException ignored){}
		System.out.println("javac: " + (javac ? "Exists" : "Nope"));
		if(!javac){
			System.out.println("Don't have javac");
			return;
		}
		System.out.println("javac: " + new ProcessBuilder("javac", temp.getAbsolutePath()).redirectError(ProcessBuilder.Redirect.PIPE).redirectOutput(ProcessBuilder.Redirect.PIPE).start().waitFor());
		System.out.println("java: " + new ProcessBuilder("java", new File(temp.getParent(), "Src").getAbsolutePath()).redirectError(ProcessBuilder.Redirect.PIPE).redirectOutput(ProcessBuilder.Redirect.PIPE).start().waitFor());
	}
}
