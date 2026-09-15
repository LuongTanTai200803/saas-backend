package com.saasai.feature.ai;


import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.policy.AbstractRenderPolicy;
import com.deepoove.poi.render.RenderContext;

import org.apache.poi.xwpf.usermodel.XWPFRun;   

public class MultiLineTextRenderPolicy extends AbstractRenderPolicy<TextRenderData> {

    @Override
    protected boolean validate(TextRenderData data) {
        return data != null;
    }

    @Override
    public void doRender(RenderContext<TextRenderData> context) throws Exception {
        XWPFRun run = context.getRun();
        String text = context.getData().getText();


        String[] lines = text.split("\\R", -1);

        run.setText("", 0);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }
    
}