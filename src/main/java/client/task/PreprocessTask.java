package client.task;

import client.option.OptionPool;
import preprocess.Preprocessor;

import java.nio.file.Path;

public class PreprocessTask extends Task {
    private final Path cFile, iFile;

    public PreprocessTask(OptionPool options, Path cFile, Path iFile) {
        super(options);
        this.cFile = cFile;
        this.iFile = iFile;
    }

    @Override
    public void run() {
        super.run();
        Preprocessor preprocessor = new Preprocessor(cFile, iFile);
        preprocessor.preprocess();
    }
}
