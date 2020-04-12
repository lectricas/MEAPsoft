 /*
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */

package com.meapsoft.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*; 
import javax.swing.border.*; 

import com.meapsoft.*;


/**
 * GUI interface for Segmenter.  
 *
 * @author Douglas Repetto (douglas@music.columbia.edu)
 * and the MEAP team
 */
public class SegmenterPanel extends MEAPsoftGUIPanel
{
	//segmenter GUI
	JCheckBox enableBox;
	JRadioButton eventStyleButton;
	JRadioButton beatStyleButton;
	JCheckBox firstFrameBox;
	JTextField inputSoundFileField;
	JLabel outputSegFileLabel;
	JSlider thresholdSlider;
    JSlider densitySlider;
    JCheckBox halfTempoBox;
    JButton listenButton;

    JPanel controlPanel;
    JPanel eventPanel;
    JPanel beatPanel;
    
    /**
     * Create a new Segmenter panel
     */
    public SegmenterPanel(MEAPsoftGUI msg)
    {
        super(msg);
        BuildSegmenterGUI();

        title = "Segmenter";
        helpURL += "#" + title;
    }

	private void BuildSegmenterGUI()
	{
		Color c = new Color((int)(Math.random() * 127 + 127),
					(int)(Math.random() * 127 + 127),
					(int)(Math.random() * 127 + 127));
        color = c;

		setBackground(c);
		BoxLayout sbl = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(sbl);
		
		JPanel segmenterEnablePanel = new JPanel();

		segmenterEnablePanel.setBackground(c);
		
		enableBox = new JCheckBox("ENABLE SEGMENTER");
		enableBox.setBackground(c);
		enableBox.setSelected(true);
		segmenterEnablePanel.add(enableBox);
		
		helpButton = new JLabel("(help)");
		//helpButton.setBackground(c.darker());
		helpButton.setForeground(Color.blue);
		helpButton.addMouseListener(this);
		segmenterEnablePanel.add(helpButton);
		
		add(segmenterEnablePanel);
		
        Box segmenterControlsPanel = Box.createVerticalBox();
		segmenterControlsPanel.setBackground(c);
		TitledBorder title = BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), 
			"Segmenter Controls");
		title.setTitleJustification(TitledBorder.CENTER);
		segmenterControlsPanel.setBorder(title);
			
		JPanel inputFileNamePanel = new JPanel();
		inputFileNamePanel.setBackground(c);
		
		JLabel inputFileNameBoxLabel = new JLabel("input sound file:");
		inputFileNamePanel.add(inputFileNameBoxLabel);
	
		inputSoundFileField = new JTextField("chris_mann.wav");
		inputSoundFileField.setColumns(20);
		inputSoundFileField.addActionListener(this);
		inputSoundFileField.setActionCommand("setInputFile");
		inputFileNamePanel.add(inputSoundFileField);
		
		JButton inputBrowseButton = new JButton("browse");
		inputBrowseButton.setBackground(c);
		inputBrowseButton.addActionListener(this);
		inputBrowseButton.setActionCommand("browseInputFile");
		inputFileNamePanel.add(inputBrowseButton);
		
		listenButton = new JButton("listen");
		listenButton.setBackground(c);
		listenButton.addActionListener(this);
		listenButton.setActionCommand("listen");
		inputFileNamePanel.add(listenButton);
		
		segmenterControlsPanel.add(inputFileNamePanel);
		
		JPanel detectorTypePanel = new JPanel();
		detectorTypePanel.setBackground(c);
		ButtonGroup onsetDetectorTypeGroup = new ButtonGroup();
		eventStyleButton = new JRadioButton("detect events");
		eventStyleButton.setBackground(c);
        eventStyleButton.addActionListener(this);
        eventStyleButton.setActionCommand("event_detector");
		beatStyleButton = new JRadioButton("detect beats");
		beatStyleButton.setBackground(c);
        beatStyleButton.addActionListener(this);
        beatStyleButton.setActionCommand("beat_detector");
		onsetDetectorTypeGroup.add(eventStyleButton);
		onsetDetectorTypeGroup.add(beatStyleButton);
		detectorTypePanel.add(eventStyleButton);
		detectorTypePanel.add(beatStyleButton);
		eventStyleButton.setSelected(true);

		segmenterControlsPanel.add(detectorTypePanel);
		

        eventPanel = new JPanel();
        eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
		eventPanel.setBackground(c);
        JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        thresholdPanel.setBackground(c);
		JLabel thresholdLabel = new JLabel("segment sensitivity: ");
		thresholdPanel.add(thresholdLabel);
		thresholdSlider = new JSlider(JSlider.HORIZONTAL, 0, 20, 1);
		thresholdSlider.setBackground(c);
		thresholdSlider.setValue(10);
		Hashtable labelTable = new Hashtable();
		labelTable.put( new Integer(0), new JLabel("low") );
		labelTable.put( new Integer(20), new JLabel("high") );
		thresholdSlider.setLabelTable( labelTable );
		thresholdSlider.setPaintLabels(true);
		thresholdSlider.setMajorTickSpacing(2);
		thresholdSlider.setPaintTicks(true);
		thresholdPanel.add(thresholdSlider);
        eventPanel.add(thresholdPanel);

        JPanel densityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        densityPanel.setBackground(c);
        JLabel densityLabel = new JLabel("segment density: ");
        densityPanel.add(densityLabel);
        densitySlider = new JSlider(JSlider.HORIZONTAL, 0, 20, 1);
        densitySlider.setBackground(c);
        densitySlider.setValue(15);
        densitySlider.setMajorTickSpacing(2);
        densitySlider.setPaintTicks(true);
		densitySlider.setLabelTable(labelTable);
		densitySlider.setPaintLabels(true);
        densityPanel.add(densitySlider);
        eventPanel.add(densityPanel);

		beatPanel = new JPanel();
        beatPanel.setBackground(c);
        halfTempoBox = new JCheckBox("cut tempo in half");
        halfTempoBox.setBackground(c);
        halfTempoBox.setSelected(false);
        beatPanel.add(halfTempoBox);

        // controlPanel is a wrapper around the event/beat detector knobs
        controlPanel = new JPanel();
		controlPanel.setBackground(c);
        controlPanel.add(eventPanel);
		segmenterControlsPanel.add(controlPanel);
		
		firstFrameBox = new JCheckBox("1st event = track start");
		firstFrameBox.setBackground(c);
		firstFrameBox.setSelected(true);
        firstFrameBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		segmenterControlsPanel.add(firstFrameBox);
	
        add(segmenterControlsPanel);

		JPanel outputSegFileNamePanel = new JPanel();
		outputSegFileNamePanel.setBackground(c);
		
		JLabel sFNL = new JLabel("output segment file: ");
		outputSegFileNamePanel.add(sFNL);
		outputSegFileLabel = new JLabel(" " + dataBaseName + ".seg ");
		outputSegFileLabel.setOpaque(true);
		outputSegFileLabel.setBackground(c.darker());
		outputSegFileNamePanel.add(outputSegFileLabel);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(outputSegFileNamePanel);
	}

	public void actionPerformed(ActionEvent arg0)
	{
		String command = arg0.getActionCommand();

		if (command.equals("listen"))
		{
            if (inputSoundFileNameFull == null)
            {
                GUIUtils.ShowDialog("You need to pick an input file!!!", GUIUtils.MESSAGE, meapsoftGUI.jframe);
                return;
            }
            PlaySoundFile(inputSoundFileNameFull);
		}
		else if (command.equals("browseInputFile"))
		{
			String names[] = GUIUtils.FileSelector(GUIUtils.OPEN, meapsoftGUI.dataDirectory, meapsoftGUI.jframe);
			
			if (names[0] == null)
				return;

			SetInputFileName(names);
		}
		else if (command.equals("setInputFile"))
		{
            String name = inputSoundFileField.getText(); 
            // default directory
            String names[] = {meapsoftGUI.dataDirectory + slash + name, name};

            // does outputFileNameField contain a full path?
            if((new File(name)).isAbsolute())
                names[0] = name;

            String[] nameSplit = name.split("["+slash+"]");
            names[1] = nameSplit[nameSplit.length-1];

			SetInputFileName(names);
		}
        else if(command.equals("event_detector"))
        {
            controlPanel.add(eventPanel);
            controlPanel.remove(beatPanel);
            RefreshGUI();
        }
        else if(command.equals("beat_detector"))
        {
            controlPanel.remove(eventPanel);
            controlPanel.add(beatPanel);
            RefreshGUI();
        }
    }
	
	public synchronized int run()
	{		
        if(!enableBox.isSelected())
            return 0;

        if (inputSoundFileNameFull == null)
        {
            GUIUtils.ShowDialog("You need to pick an input file!!!", GUIUtils.MESSAGE, meapsoftGUI.jframe);
            return -1;
        }

        // for event detector:
        //want value to be between 0 and 3
        double thresh = 3.0-thresholdSlider.getValue()/6.666;
        // want value between 1 and 0
        double smtime = (20.0-densitySlider.getValue())/20.0;

        // for beat detector:
        double tempoMult = 1.0;
        if(halfTempoBox.isSelected())
            tempoMult = 0.5;

		boolean beatOnsetDetector = beatStyleButton.isSelected();
		boolean firstFrameOnset = firstFrameBox.isSelected();
		
		String segmentsFileName = dataDirectory + slash + outputSegmentsFileName;
		
		Segmenter segmenter = new Segmenter(inputSoundFileNameFull,
			segmentsFileName, thresh, smtime, beatOnsetDetector, firstFrameOnset);
        segmenter.setTempoMultiplier(tempoMult);
        segmenter.writeMEAPFile = meapsoftGUI.writeMEAPFile;

        JPanel progressPanel = new JPanel();
        progressPanel.add(new JLabel("Segmenting: "));
        JProgressBar progressBar = new JProgressBar(segmenter.getProgress());
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar);
        meapsoftGUI.setProgressPanel(progressPanel);

        try
        {
            segmentFile = segmenter.processAudioFile(inputSoundFileNameFull);

            if(segmenter.writeMEAPFile)
                segmentFile.writeFile();

            segmentFile = segmenter.getSegFile();
        }
        catch (Exception e)
        {
            GUIUtils.ShowDialog(e, "Error running Segmenter", GUIUtils.MESSAGE, meapsoftGUI.jframe);

            return -1;
        }

        //System.out.println("found "+segmentFile.chunks.size()+" chunks.");

        return 0;
	}
}
