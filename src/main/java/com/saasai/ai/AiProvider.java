package com.saasai.ai;

import java.io.IOException;
import java.util.List;

public interface AiProvider {
    List<String> streamCompletion(String prompt, String model) throws IOException;
}
