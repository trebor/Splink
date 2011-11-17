package org.trebor.splink;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

public class TestMaximumSizeTextArea
{
  public static void main(String[] args)
  {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setPreferredSize(new Dimension(200, 200));
    
    frame.getContentPane().setLayout(new BorderLayout());
    final MaximumSizeTextArea msta = new MaximumSizeTextArea("test <font color=green>test</font>\ntest test");
    
    msta.setBackground(Color.RED);
    msta.setForeground(Color.BLUE);
    frame.getContentPane().add(msta, BorderLayout.CENTER);
    JButton b = new JButton("change");
    b.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent arg0)
      {
        msta.setText("foo\n" + msta.getOriginalText() + "\nmore!");
      }
    });
    
    frame.getContentPane().add(b, BorderLayout.SOUTH);

    frame.pack();
    frame.setVisible(true);
  }
}
