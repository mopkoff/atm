package utilities;

import model.BanknoteMap;

import java.io.*;
import java.rmi.NotBoundException;
import java.util.stream.Stream;

public class StreamDispatcher {

    private InputStream inStream;
    private PrintStream outStream;
    private PrintStream logStream;
    private PrintStream errStream;

    public StreamDispatcher(InputStream inStream,
                             PrintStream outStream,
                             PrintStream logStream,
                             PrintStream errStream)  {
        this.inStream = inStream;
        this.outStream = outStream;
        this.logStream = logStream;
        this.errStream = errStream;
    }


    public static PrintStream getPrintStreamForFile(String filename){
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            file.createNewFile();
            return new PrintStream(file);
        } catch (IOException e) {
            e.printStackTrace();
            return System.out;
        }
    }

    public static InputStream getInputStreamFromFile(String filename){
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            file.createNewFile();
            return new FileInputStream(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return System.in;
        }
    }

    public void overrideSystemIn(){
        System.setIn(this.inStream);
    }
    public void overrideSystemOut(){
        System.setOut(this.outStream);
    }
    public void overrideSystemErr(){
        System.setErr(this.errStream);
    }
    public void overrideSystemAll(){
        overrideSystemIn();
        overrideSystemOut();
        overrideSystemErr();
    }
    public InputStream getInStream() {
        return inStream;
    }

    public void setInStream(InputStream inStream) {
        this.inStream = inStream;
    }

    public PrintStream getOutStream() {
        return outStream;
    }

    public void setOutStream(PrintStream outStream) {
        this.outStream = outStream;
    }

    public PrintStream getLogStream() {
        return logStream;
    }

    public void setLogStream(PrintStream logStream) {
        this.logStream = logStream;
    }

    public PrintStream getErrStream() {
        return errStream;
    }

    public void setErrStream(PrintStream errStream) {
        this.errStream = errStream;
    }

}
