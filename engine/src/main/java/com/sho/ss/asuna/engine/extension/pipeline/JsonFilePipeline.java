package com.sho.ss.asuna.engine.extension.pipeline;


import com.alibaba.fastjson.JSON;
import com.sho.ss.asuna.engine.core.ResultItems;
import com.sho.ss.asuna.engine.core.Task;
import com.sho.ss.asuna.engine.core.pipeline.Pipeline;
import com.sho.ss.asuna.engine.core.utils.FilePersistentBase;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Store results to files in JSON format.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class JsonFilePipeline extends FilePersistentBase implements Pipeline
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * new JsonFilePageModelPipeline with default path "/data/webmagic/"
     */
    public JsonFilePipeline() {
        setPath("/data/webmagic");
    }

    public JsonFilePipeline(String path) {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
        try {
            PrintWriter printWriter = new PrintWriter(new FileWriter(getFile(path + DigestUtils.md5Hex(resultItems.getRequest().getUrl()) + ".json")));
            printWriter.write(JSON.toJSONString(resultItems.getAll()));
            printWriter.close();
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
    }
}
