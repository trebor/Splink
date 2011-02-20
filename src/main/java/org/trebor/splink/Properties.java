package org.trebor.splink;


import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * The Properties class is an extension of the stock Properties class which provides
 * automatic property persistence.
 */

@SuppressWarnings("serial")
public class Properties extends java.util.Properties {
  
    // file for persistent storage of properties

    private File file;

    /**
     * Constructs a Properties with specified file name.
     *
     * @param filename the file for persistent storage of properties
     */
    public Properties(String filename) {
        try {
            file = new File(filename);
            if (file.exists()) {
              {
                load(new FileInputStream(file));
                System.out.println("load!");
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the value of a property in the property database, and
     * updates the state of the properties file.
     *
     * @param name  the name of the property
     * @param value the string value of the property
     */
    public Object setProperty(String name, String value) {
        Object oldValue = super.setProperty(name, value);
        try {
            store(new FileOutputStream(file), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldValue;
    }

    /** Test that a given property exists.
     *
     * @return true if the property appears in this set of properties
     */
    public boolean exists(String name) {
        return getProperty(name) != null;
    }

    /**
     * Defines a int property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, int defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a boolean property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, boolean defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a double property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, double defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a String property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, String defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a Color property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, Color defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a Point property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, Point defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a Dimension property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, Dimension defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Defines a Rectangle property.  If the property does not exist, it will be
     * created with the specified default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that this property will take, if
     * it does not already appear in the properties list.
     */
    public void define(String name, Rectangle defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    public void define(String name, ArrayList<String> defaultValue) {
        String stringValue = getProperty(name);
        if (stringValue == null) {
            set(name, defaultValue);
        }
    }

    /**
     * Sets a int property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, int value) {
        setProperty(name, "" + value);
    }

    /**
     * Sets a boolean property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, boolean value) {
        setProperty(name, value ? "TRUE" : "FALSE");
    }

    /**
     * Sets a double property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, double value) {
        setProperty(name, "" + value);
    }

    /**
     * Sets a String property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, String value) {
        setProperty(name, value);
    }

    /**
     * Sets a Color property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, Color value) {
        setProperty(name,
                (int) value.getRed() + ", " +
                (int) value.getGreen() + ", " +
                (int) value.getBlue() + ", " +
                (int) value.getAlpha());
    }

    /**
     * Sets a Point property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, Point value) {
        setProperty(name,
                (int) value.getX() + ", " +
                (int) value.getY());
    }

    /**
     * Sets a Dimension property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, Dimension value) {
        setProperty(name, (int) value.getWidth() + ", " + (int) value.getHeight());
    }

    /**
     * Sets a Rectangle property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name  the name of the property
     * @param value the value that this property will take
     */
    public void set(String name, Rectangle value) {
        setProperty(name,
                (int) value.getX() + ", " +
                (int) value.getY() + ", " +
                (int) value.getWidth() + ", " +
                (int) value.getHeight());
    }


    /**
     * Sets a Font property.  If the property does not exist, it will be
     * created with the specified value.
     *
     * @param name the name of the property
     * @param value the value that this property will take
     */
    
    public void set(String name, Font font) {
        setProperty(name, String.format("%s-%d", font.getFontName(), font.getSize()));
    }

    public void set(String name, ArrayList<String> value) {
        String list = "";

        for (int i = 0; i < value.size(); i++) {
            list += value.get(i) + ", ";
        }

        if (!list.equals("")) {
            list = list.substring(0, list.length() - 2);
        }

        setProperty(name, list);
    }

    /**
     * Gets an integer property value.
     *
     * @return the integer value of property if it exists, otherwise it
     * throws an exception.
     */
    public int getInteger(String name) {
        return Integer.valueOf(getProperty(name));
    }

    /**
     * Gets a boolean property value.
     *
     * @return the boolean value of property if it exists, otherwise it
     * throws an exception.
     */
    public boolean getBoolean(String name) {
        return Boolean.valueOf(getProperty(name));
    }

    /**
     * Gets a double property value.
     *
     * @return the double value of property if it exists, otherwise it
     * throws an exception.
     */
    public double getDouble(String name) {
        return Double.valueOf(getProperty(name));
    }

    /**
     * Gets a String property value.
     *
     * @return the String value of property if it exists, otherwise it
     * throws an exception.
     */
    public String getString(String name) {
        return getProperty(name);
    }

    /**
     * Gets a Color property value.
     *
     * @return the Color value of property if it exists, otherwise it
     * throws an exception.
     */
    public Color getColor(String name) {
        String[] values = getProperty(name).split(",");
        return new Color(
                Integer.valueOf(values[0].trim()),
                Integer.valueOf(values[1].trim()),
                Integer.valueOf(values[2].trim()),
                Integer.valueOf(values[3].trim()));
    }

    /**
     * Gets a Point property value.
     *
     * @return the Point value of property if it exists, otherwise it
     * throws an exception.
     */
    public Point getPoint(String name) {
        String[] values = getProperty(name).split(",");
        return new Point(
                Integer.valueOf(values[0].trim()),
                Integer.valueOf(values[1].trim()));
    }

    /**
     * Gets a Dimension property value.
     *
     * @return the Dimension value of property if it exists, otherwise it
     * throws an exception.
     */
    public Dimension getDimension(String name) {
        String[] values = getProperty(name).split(",");
        return new Dimension(
                Integer.valueOf(values[0].trim()),
                Integer.valueOf(values[1].trim()));
    }

    /**
     * Gets a Rectangle property value.
     *
     * @return the Rectangle value of property if it exists, otherwise it
     * throws an exception.
     */
    public Rectangle getRectangle(String name) {
        String[] values = getProperty(name).split(",");
        return new Rectangle(
                Integer.valueOf(values[0].trim()),
                Integer.valueOf(values[1].trim()),
                Integer.valueOf(values[2].trim()),
                Integer.valueOf(values[3].trim()));
    }

    /**
     * Gets a Font property value.
     *
     * @return the Font value of property if it exists, otherwise it
     * throws an exception.
     */

    public Font getFont(String name)
    {
      return Font.decode(getProperty(name));
    }
}
