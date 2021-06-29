package com.sandeep.api.config;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class PropertyFileReader {
    private Properties property;

    public PropertyFileReader(File propertyFilePath) {
        if (propertyFilePath.exists() && propertyFilePath.isFile()) {
            try {
                property = new Properties();

                property.load(new FileInputStream(propertyFilePath));
            } catch (IOException e) {
                System.out.println("Unable to locate the file at the provided location " + propertyFilePath.getPath());
                e.printStackTrace();
            }
        }
    }

    public Properties getPropertyFile() {
        return property;
    }
}
