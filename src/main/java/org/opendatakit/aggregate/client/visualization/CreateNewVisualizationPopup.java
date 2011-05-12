/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.client.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendatakit.aggregate.client.form.FormServiceAsync;
import org.opendatakit.aggregate.client.form.KmlSettingOption;
import org.opendatakit.aggregate.client.form.KmlSettings;
import org.opendatakit.aggregate.client.preferences.Preferences;
import org.opendatakit.aggregate.client.submission.Column;
import org.opendatakit.aggregate.client.submission.SubmissionServiceAsync;
import org.opendatakit.aggregate.client.submission.SubmissionUI;
import org.opendatakit.aggregate.client.submission.UIGeoPoint;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CreateNewVisualizationPopup extends PopupPanel {
	private static final String UI_PIE_CHART = "Pie Chart";
	private static final String UI_BAR_GRAPH = "Bar Graph";
	private static final String UI_SCATTER_PLOT = "Scatter Plot";
	private static final String UI_MAP = "Map";
	
	private static final String GOOGLE_PIE_CHART = "p3";
	private static final String GOOGLE_BAR_GRAPH = "bvg";
	private static final String GOOGLE_SCATTER_PLOT = "s";
	
	private static final HashMap<String, String> UI_TO_GOOGLE = new HashMap<String, String>();
	static {
		UI_TO_GOOGLE.put(UI_PIE_CHART, GOOGLE_PIE_CHART);
		UI_TO_GOOGLE.put(UI_BAR_GRAPH, GOOGLE_BAR_GRAPH);
		UI_TO_GOOGLE.put(UI_SCATTER_PLOT, GOOGLE_SCATTER_PLOT);
	}
	
	private final List<Column> head;
	private final List<SubmissionUI> sub;
	private final FlexTable dropDownsTable = new FlexTable();
	private final ListBox chartType = new ListBox();
	private final ListBox firstData = new ListBox();
	private final ListBox secondData = new ListBox();
	private final Image chart = new Image();
	private FlowPanel mapSpace = new FlowPanel();
	private boolean mapsApiLoaded = false;
	private List<KmlSettingOption> geoPoints = new ArrayList<KmlSettingOption>();
	
	private String getImageUrl() {
		StringBuffer chartUrl = new StringBuffer("https://chart.googleapis.com/chart?cht=");
		chartUrl.append(UI_TO_GOOGLE.get(chartType.getItemText(chartType.getSelectedIndex())));
		chartUrl.append("&chs=600x500");
		
		int firstIndex = 0;
		int secondIndex = 0;
		String firstDataValue = firstData.getItemText(firstData.getSelectedIndex());
		String secondDataValue = secondData.getItemText(secondData.getSelectedIndex());
      chartUrl.append("&chtt=" + secondDataValue);
      chartUrl.append("&chxt=x,y");
		int index = 0;
		for (Column c : head) {
			if (c.getDisplayHeader().equals(firstDataValue))
				firstIndex = index;
			if (c.getDisplayHeader().equals(secondDataValue))
				secondIndex = index;
			index++;
		}
		StringBuffer firstValues = new StringBuffer();
		StringBuffer secondValues = new StringBuffer();
		for (SubmissionUI s : sub) {
			firstValues.append(s.getValues().get(firstIndex));
			firstValues.append("|");
			secondValues.append(s.getValues().get(secondIndex));
			secondValues.append(",");
		}
		if (firstValues.length() > 0)
			firstValues.delete(firstValues.length() - 1, firstValues.length());
		if (secondValues.length() > 0)
			secondValues.delete(secondValues.length() - 1, secondValues.length());
		chartUrl.append("&chd=t:");
		chartUrl.append(secondValues.toString());
		chartUrl.append("&chdl=");
		chartUrl.append(firstValues.toString());
		
		
		return chartUrl.toString();
	}
	
	public void enableMap() {
		firstData.setEnabled(false);
		chart.setVisible(false);
		mapSpace.setVisible(true);
		secondData.clear();
		for (KmlSettingOption kSO : geoPoints)
			secondData.addItem(kSO.getDisplayName());
	}
	
	public void disableMap() {
		firstData.setEnabled(true);
		mapSpace.setVisible(false);
		chart.setVisible(true);
		secondData.clear();
		for (Column c : head)
			secondData.addItem(c.getDisplayHeader());
	}
	
	public CreateNewVisualizationPopup(List<Column> headers,
									   List<SubmissionUI> submissions,
									   final String formId,
									   final FormServiceAsync formSvc,
									   final SubmissionServiceAsync submissionSvc) {
		super(false);
		this.head = headers;
		this.sub = submissions;
	    
	    Maps.loadMapsApi(Preferences.getGoogleMapsApiKey(), "2", false, new Runnable() {
	      public void run() {
	        mapsApiLoaded = true;
	      }
	    });
	    
	    formSvc.getGpsCoordnates(formId, new AsyncCallback<KmlSettings>() {
	        public void onFailure(Throwable c) {}
	        public void onSuccess(KmlSettings result) {
	          geoPoints = result.getGeopointNodes();
	        }
	      });
		
		VerticalPanel layoutPanel = new VerticalPanel();
		
		for (Column c : head) {
			firstData.addItem(c.getDisplayHeader());
			secondData.addItem(c.getDisplayHeader());
		}
		
		final Button executeButton = new Button("<img src=\"images/pie_chart.png\" /> Pie It");
		executeButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (chartType.getItemText(chartType.getSelectedIndex()).equals(UI_MAP)) {
					submissionSvc.getGeoPoints(formId, geoPoints.get(secondData.getSelectedIndex()).getElementKey(),
							new AsyncCallback<UIGeoPoint[]>() {
								@Override
								public void onFailure(Throwable caught) {}
								
								@Override
								public void onSuccess(UIGeoPoint[] result) {
									if (!mapsApiLoaded)
										return;
									LatLng center = null;
									if (result == null || result.length == 0) {
										center = LatLng.newInstance(0.0, 0.0);
									} else {
										for (int i = 0; i < result.length; i++) {
											try {
												System.out.println("["+result[i].getLatitude() + ", "+result[i].getLongitude()+"]");
												center = LatLng.newInstance(Double.parseDouble(result[i].getLatitude()), Double.parseDouble(result[i].getLongitude()));
												break;
											} catch (NumberFormatException e) {
											} catch (NullPointerException e) {}
										}
									}
									if (center == null)
										return;
									final MapWidget map = new MapWidget(center, 2);
									map.setSize("100%", "100%");
									map.addControl(new LargeMapControl());
									mapSpace.add(map);
									
									for (UIGeoPoint u : result) {
										if (u == null || u.getLatitude() == null || u.getLongitude() == null)
											continue;
										try {
											LatLng ll = LatLng.newInstance(Double.parseDouble(u.getLatitude()), Double.parseDouble(u.getLongitude()));
											map.addOverlay(new Marker(ll));
										} catch (NumberFormatException e) {
											continue;
										}
									}
								}
							});
				} else {
					chart.setUrl(getImageUrl());
				}
			}
		});
		
		chartType.addItem(UI_PIE_CHART);
		chartType.addItem(UI_BAR_GRAPH);
		chartType.addItem(UI_MAP);
		//chartType.addItem(UI_SCATTER_PLOT);
		
		chartType.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				String selected = chartType.getValue(chartType.getSelectedIndex());
				if (selected.equals(UI_MAP)) {
					executeButton.setHTML("<img src=\"images/map.png\" /> Map It");
					enableMap();
				} else {
					if (selected.equals(UI_PIE_CHART)) {
						executeButton.setHTML("<img src=\"images/pie_chart.png\" /> Pie It");
					} else if (selected.equals(UI_BAR_GRAPH)) {
						executeButton.setHTML("<img src=\"images/bar_chart.png\" /> Bar It");
					} else { // selected.equals(UI_SCATTER_PLOT)
						executeButton.setHTML("<img src=\"scatter_plot.png\" /> Plot It");
					}
					disableMap();
				}
			}
		});
		
		Button closeButton = new Button("<img src=\"images/red_x.png\" />");
		closeButton.addStyleDependentName("close");
		closeButton.addStyleDependentName("negative");
		closeButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		
		dropDownsTable.setWidget(0, 0, chartType);
		dropDownsTable.setText(0, 1, "Labels:");
		dropDownsTable.setWidget(0, 2, firstData);
		dropDownsTable.setText(0, 3, "Data:");
		dropDownsTable.setWidget(0, 4, secondData);
		dropDownsTable.setWidget(0, 5, executeButton);
		dropDownsTable.setWidget(0, 6, closeButton);
		dropDownsTable.addStyleName("popup_menu");
		dropDownsTable.getCellFormatter().addStyleName(0, 6, "popup_close_cell");
		
		layoutPanel.add(dropDownsTable);
		chart.getElement().setId("chart_image");
		chartType.setItemSelected(0, true);
		layoutPanel.add(chart);
		layoutPanel.add(mapSpace);
		mapSpace.getElement().setId("map_area");
		disableMap();
		
	    formSvc.getGpsCoordnates(formId, new AsyncCallback<KmlSettings>() {
	      public void onFailure(Throwable c) {}
	      public void onSuccess(KmlSettings result) {
	        geoPoints = result.getGeopointNodes();
	      }
	    });
		
		setWidget(layoutPanel);
	}
}