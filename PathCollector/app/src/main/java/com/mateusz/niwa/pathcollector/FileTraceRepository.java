package com.mateusz.niwa.pathcollector;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileTraceRepository implements ITraceRepository {
    static final File DEFAULT_LOCATION;

    static {
        DEFAULT_LOCATION = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "pathcollector");
    }

    private File mRepositoryRoot;
    private Map<String, Map<Long, Trace>> mRepository;

    FileTraceRepository() {
        mRepositoryRoot = DEFAULT_LOCATION;
        mRepository = new HashMap<String, Map<Long, Trace>>();
    }

    FileTraceRepository(File location) {
        mRepositoryRoot = location;
        mRepository = new HashMap<String, Map<Long, Trace>>();
    }

    public void addTrace(Trace trace, String tag) {
        if (! mRepository.containsKey(tag))
            mRepository.put(tag, new HashMap<Long, Trace>());

        if (! mRepository.get(tag).containsKey(trace.getID()))
            mRepository.get(tag).put(trace.getID(), trace);
    }

    public void pull() {
        List<File> repositorySubdirs = getSubdirectoryList(mRepositoryRoot);

        for (File subdir : repositorySubdirs) {
            List<File> files = getFileList(subdir);

            for (File file : files) {
                String xml;
                try {
                    xml = readFile(file);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot read file " + file.getName());
                }

                addTrace(new Trace(xml), subdir.getName());
            }
        }
    }

    public void push() {
        Set<String> dirnames = new HashSet<String>();

        if (mRepositoryRoot.exists())
            for (File subdir : getSubdirectoryList(mRepositoryRoot))
                dirnames.add(subdir.getName());
        else
            mRepositoryRoot.mkdirs();

        for (Map.Entry<String, Map<Long, Trace>> tagEntry : mRepository.entrySet()) {
            File currentSubdir = new File(mRepositoryRoot, tagEntry.getKey());

            if (! dirnames.contains(tagEntry.getKey()))
                currentSubdir.mkdirs();

            Set<String> filenames = new HashSet<String>();

            for (File file : getFileList(mRepositoryRoot))
                filenames.add(file.getName());

            for (Map.Entry<Long, Trace> traceEntry : tagEntry.getValue().entrySet()) {
                if (! filenames.contains(traceEntry.getKey().toString())) {
                    try {
                        FileOutputStream stream = new FileOutputStream(new File(currentSubdir, traceEntry.getKey().toString() + ".trc"));
                        stream.write(traceEntry.getValue().toXml().getBytes());
                        stream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private List<File> getFileList(File parentDir) {
        ArrayList<File> foundFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();

        if (files == null)
            return foundFiles;

        for (File file : files) {
            if (! file.isDirectory())
                foundFiles.add(file);
        }

        return foundFiles;
    }

    private List<File> getSubdirectoryList(File parentDir) {
        ArrayList<File> foundDirectories = new ArrayList<File>();
        File[] files = parentDir.listFiles();

        if (files == null)
            return foundDirectories;

        for (File file : files) {
            if (file.isDirectory())
                foundDirectories.add(file);
        }

        return foundDirectories;
    }

    private String readFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder(1024);
        String line = null;
        Boolean firstLine = true;

        while ( (line = reader.readLine()) != null ) {
            if (firstLine) {
                builder.append(line);
                firstLine = false;
            } else
                builder.append("\n").append(line);
        }

        reader.close();
        stream.close();
        return builder.toString();
    }
}
