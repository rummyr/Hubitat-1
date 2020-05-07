/**
 *  Hubigraph Timeline Child App
 *
 *  Copyright 2020, but let's behonest, you'll copy it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

// Hubigraph Line Graph Changelog
// V 0.1 Intial release
// V 0.2 Fixed startup code which needed all three device types, now one will work
// V 0.22 Update to support tiles
// V 0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
 
import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Time Line",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Time Line",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", title: "Main Page", install: true, uninstall: true)
       page(name: "deviceSelectionPage")
       page(name: "graphSetupPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage")
    }
   

mappings {
    path("/graph/") {
            action: [
              GET: "getGraph"
            ]
        }
    }
    
    path("/getData/") {
        action: [
            GET: "getData"
        ]
    }
        
    path("/getOptions/") {
        action: [
            GET: "getOptions"
        ]
    }
    
    path("/getSubscriptions/") {
        action: [
            GET: "getSubscriptions"
        ]
    }
}

def deviceSelectionPage() {
          
    dynamicPage(name: "deviceSelectionPage") {
        section() { 
            input (type: "capability.switch", name: "switches", title: "Choose Switches", multiple: true);
            input (type: "capability.motionSensor", name: "motions", title: "Choose Motion Sensors", multiple: true);
            input (type: "capability.contactSensor", name: "contacts", title: "Choose Contact Sensors", multiple: true);
            input (type: "capability.presenceSensor", name: "presences", title: "Choose Presence Sensors", multiple: true);
        }
    }
}


def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def colorEnum = ["Maroon", "Red", "Orange", "Yellow", "Olive", "Green", "Purple", "Fuchsia", "Lime", "Teal", "Aqua", "Blue", "Navy", "Black", "Gray", "Silver", "White", "Transparent"];
    
    dynamicPage(name: "graphSetupPage") {
        section(){
            paragraph getTitle("General Options");
            input( type: "enum", name: "graph_update_rate", title: "Select graph update rate", multiple: false, required: false, options: [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]], defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "Select Timespan to Graph", multiple: false, required: false, options: [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]], defaultValue: "43200000")     
            input( type: "enum", name: "graph_background_color", title: "Background Color", defaultValue: "White", options: colorEnum);
            
            //Size
            paragraph getTitle("Graph Size");
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
            
            //Axis
            paragraph getTitle("Axes");
            input( type: "enum", name: "graph_axis_font", title: "Graph Axis Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_axis_color", title: "Graph Axis Text Color", defaultValue: "Black", options: colorEnum);
        }
    }
}

def disableAPIPage() {
    dynamicPage(name: "disableAPIPage") {
        section() {
            if (state.endpoint) {
                try {
                   revokeAccessToken();
                }
                catch (e) {
                    log.debug "Unable to revoke access token: $e"
                }
                state.endpoint = null
            }
            paragraph "It has been done. Your token has been REVOKED. Tap Done to continue."
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage", title: "") {
        section() {
            if(!state.endpoint) initializeAppEndpoint();
            if (!state.endpoint){
                paragraph "Endpoint creation failed"
            } else {
                paragraph "It has been done. Your token has been CREATED. Tap Done to continue."
            }
        }
    }
}

def getTimeString(string_){
    //log.debug("Looking for $string_")
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 300000:return "5 Minutes"; 
        case 600000:return "10 Minutes"; 
        case 1800000:return "Half Hour";
        case 3600000:return "1 Hour";
    }
    log.debug("NOT FOUND");
}

def getTimsSpanString(string_){
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 3600000:return "1 Hour";
        case 43200000: return "12 Hours";
        case 86400000: return "1 Day";
        case 259200000: return "3 Days";
        case 604800000: return "1 Week";
    }
    log.debug("NOT FOUND");
}


def mainPage() {
    def timeEnum = [["0":"Never"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], 
                    ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]]
    
    dynamicPage(name: "mainPage") {        
        section(){
            if (!state.endpoint) {
                paragraph "API has not been setup. Tap below to enable it."
                href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
            } else {
                href name: "deviceSelectionPage", title: "Select Device/Data", description: "", page: "deviceSelectionPage" 
                href name: "graphSetupPage", title: "Configure Graph", description: "", page: "graphSetupPage"
                paragraph getLine();
                paragraph "<i><u><b>LOCAL GRAPH URL</b></u></i>\n${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
         
                
                    def paragraph_ = /<table width="100%" ID="Table2" style="margin: 0px;">/
                    paragraph_ +=  "${getTableRow3("<i><u><b>DEVICE INFORMATION</b></u></i>", "","","")}"
                    paragraph_ +=  "${getTableRow3("<i><u>SWITCH</u></i>","<i><u>MOTION</u></i>","<i><u>CONTACTS</u></i>","<i><u>PRESENCE</u></i>")}" 
                    num_switches = switches ?  switches.size() : 0;
                    num_motions = motions ?  motions.size() : 0;
                    num_contacts = contacts ?  contacts.size() : 0;
                    num_presences = presences ?  presences.size() : 0;
                    max_ = Math.max(num_switches, Math.max(num_motions, Math.max(num_contacts, num_presences)));
                
                    log.debug ("Max = $max_, $num_switches, $num_motions, $num_contacts, $num_presences");
                    for (i=0; i<max_; i++){
                        switchText = i<num_switches?switches[i]:"";
                        motionText = i<num_motions?motions[i]:"";
                        contactText = i<num_contacts?contacts[i]:"";
                        presenceText = i<num_presences?presences[i]:"";
                        paragraph_ += /${getTableRow3("$switchText", "$motionText", "$contactText", "$presenceText" )}/
                    }    
                    paragraph_ += "</table>"
                    paragraph paragraph_
                 
                if (graph_timespan){
                    
                
                    def timeString = getTimsSpanString(graph_timespan);
                    graph_update_rate = graph_update_rate ? graph_update_rate : 0
                    paragraph_ =  "<table>"
                    paragraph_ += "${getTableRow("<b><u>GRAPH SELECTIONS</b></u>", "<b><u>VALUE</b></u>", "<b><u>SIZE</b></u>", "<b><u>COLOR</b></u>")}"
                    paragraph_ += /${getTableRow("Timespan", timeString,"","")}/
                    paragraph_ += /${getTableRow("Update Rate", getTimeString(graph_update_rate), "", "")}/ 
                    if (graph_static_size==true){
                        paragraph_ += /${getTableRow("Graph Size", "$graph_h_size X $graph_v_size", "","")}/
                    } else {
                        paragraph_ += /${getTableRow("Graph Size", "DYNAMIC", "","")}/
                    }
                    paragraph_ += /${getTableRow("Axis", "", graph_axis_font, graph_axis_color)}/
                    paragraph_ += /${getTableRow("Background", "", "", graph_background_color)}/
                    paragraph_ += "</table>"
                    paragraph paragraph_
                } //graph_timespan
            }//else
        }
        
        section(){
            if (state.endpoint){
                paragraph getLine();
                input( type: "text", name: "app_name", title: "<b>Rename the Application?</b>", default: "Hubigraph Line Graph", submitOnChange: true ) 
                href url: "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}", title: "Graph -- Please Click <span style='font-weight: bold; font-size: 12px; padding: 10px; border-radius: 3px; box-shadow: 1px 1px 5px -2px black; margin: 5px;'>Done</span> to save settings before viewing the graph"
                href "disableAPIPage", title: "Disable API", description: ""
            }
        }    
        
    }
}

def getLine(){	  
	def html = "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    html
}

def getTableRow(col1, col2, col3, col4){
     def html = "<tr><td width='40%'>$col1</td><td width='30%'>$col2</td><td width='20%'>$col3</td><td width='10%'>$col4</td></tr>"  
     html
}

def getTableRow3(col1, col2, col3, col4){
     def html = "<tr><td width='23%'>$col1</td><td width='23%'>$col2</td><td width='23%'>$col3</td><td width='31%'>$col4</td></tr>"  
     html
}

def getTitle(myText=""){
    def html = "<div class='row-full' style='background-color:#1A77C9;color:white;font-weight: bold'>"
    html += "${myText}</div>"
    html
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    updated();
}

def uninstalled() {
    if (state.endpoint) {
        try {
            logDebug "Revoking API access token"
            revokeAccessToken()
        }
        catch (e) {
            log.warn "Unable to revoke API access token: $e"
        }
    }
}

def updated() {
    app.updateLabel(app_name);
    state.dataName = attribute;  
}

def getStartEventString(type) {
    switch (type){
         case "switch": return "on";
         case "motion": return "active";
         case "contact": return "open";
         case "presence": return "present";
    }
}

def getEndEventString(type) {
    switch (type){
         case "switch": return "off";
         case "motion": return "inactive";
         case "contact": return "closed";
         case "presence": return "not present";
    }
}

def buildData(){
    def data = []; 
    if (switches) data += buildDataCapability(switches, "switch");
    if (motions) data += buildDataCapability(motions, "motion");
    if (contacts) data += buildDataCapability(contacts, "contact");
    if (presences) data += buildDataCapability(presences, "presence");
    return data;
}

def buildDataCapability(device_type, capability_) {
    def resp = []
    def now = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    def start_event = getStartEventString(capability_);
    def end_event = getEndEventString(capability_);
    
    log.debug("Initializing:: Capability = $capability_ from $then to $now");
    
    if(device_type) {
      device_type.each {it->  
          temp = it?.eventsBetween(then, now, [max: 500000])?.findAll{it.value == start_event || it.value == end_event}?.collect{[date: it.date, value: it.value]}
          temp = temp.sort{ it.date };
          temp = temp.collect{ [date: it.date.getTime(), value: it.value] }
          
          //Final Parsing
          //firstTime = true;
          //temp_start = null;
          //place a short "start event to force the graph to the right range
          finalList = [];
          
          if(temp.size() > 0) {
              //if our first event is an end event, start at 1
              for(int i = temp[0].value == start_event ? 0 : 1; i < temp.size() - 1; i += 2) {
                  if(temp[i].date < temp[i + 1].date) finalList << [start: temp[i].date, end: temp[i + 1].date];
              }
          
              //add orphaned nodes
              if(temp[0].value != start_event) {
                  finalList.add(0, [end: temp[0].date]);
              } else if (temp[temp.size() - 1].value != end_event) {
                  finalList << [start: temp[temp.size() - 1].date];
              }
          }
          //if it's already on, add an event
          else if(it.currentState(capability_).value.equals(start_event)) {
              finalList << [start: then.getDate()];
          }
          
          resp << [id_:it.deviceId, events_:finalList];
      }
      
   }
   
   
   return resp
}

def getChartOptions(){
    def options = [
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "timeline": [
                "rowLabelStyle": ["fontSize": graph_axis_font, "color": getColorCode(graph_axis_color)],
                "barLabelStyle": ["fontSize": graph_axis_font]
            ],
            "backgroundColor": getColorCode(graph_background_color)
        ]
    ]
    
    return options;
}
        
void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
}

def getTimeLine() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <head>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
google.charts.load('current', {'packages':['timeline']});
google.charts.setOnLoadCallback(onLoad);
            
let options = [];
let subscriptions = {};
let graphData = {};

let websocket;

function getOptions() {
    return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Options");
        console.log(data);                        
        options = data;
    });
}

function getSubscriptions() {
    return jQuery.get("${state.localEndpointURL}getSubscriptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Subscriptions");
        console.log(data);
        subscriptions = data;
        
    });
}

function getGraphData() {
    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Graph Data");
        console.log(data);
        graphData = data;
        
    });
}

function parseEvent(event) {
    const now = new Date().getTime();

    function getIsStart() {
        if(event.name == "switch") return event.value == "on";
        else if (event.name == "motion") return event.value == "active";
        else if (event.name == "contact") return event.value == "open";
        else if (event.name == "presence") return event.value == "present";
    }

    let deviceId = event.deviceId;

    //only accept relevent events
    let deviceIndex = -1;
    Object.entries(subscriptions).forEach(([key, val]) => {
        //if we are subscribed to this certain type
        if(val) {
            let index = val.findIndex((it) => it.idAsLong === deviceId);
            if(index != -1) deviceIndex = index;
        }
    });

    if(deviceIndex != -1) {
        let isStart = getIsStart();
        let pastEvents = graphData[deviceId];
        if(pastEvents.length > 0) {
            if(!isStart && !pastEvents[pastEvents.length - 1].end) pastEvents[pastEvents.length - 1].end = now;
            //if we have an end event last
            else if(isStart && pastEvents[pastEvents.length - 1].end) {
                pastEvents.push({ start: now });
            }
        } else {
            pastEvents.push({ start: now });
        }

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

async function update() {
    let now = new Date().getTime();
    let min = now;
    min -= options.graphTimespan;

    //boot old data
    Object.entries(graphData).forEach(([name, arr]) => {
        //shift left points and mark for deletion if applicable
        let newArr = arr.map(it => {
            let ret = { ...it }

            if(it.end && it.end < min) {
                ret = {};
            }
            else if(it.start && it.start < min) ret.start = min;
            

            return ret;
        });

        //delete non-existant nodes
        newArr = newArr.filter(it => it.start || it.end);

        graphData[name] = newArr;
    });

    drawChart(now, min);
}

async function onLoad() {
    //first load
    await getOptions();
    await getSubscriptions();
    await getGraphData();

    update();

    //start our update cycle
    if(options.graphUpdateRate !== -1) {
        //start websocket
        websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
        websocket.onopen = () => {
            console.log("WebSocket Opened!");
        }
        websocket.onmessage = (event) => {
            parseEvent(JSON.parse(event.data));
        }

        if(options.graphUpdateRate !== 0) {
            setInterval(() => {
                update();
            }, options.graphUpdateRate);
        }
    }

    //attach resize listener
    window.addEventListener("resize", () => {
        let now = new Date().getTime();
        let min = now;
        min -= options.graphTimespan;

        drawChart(now, min);
    });
}

function drawChart(now, min) {
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ type: 'string', id: 'Device' });
    dataTable.addColumn({ type: 'date', id: 'Start' });
    dataTable.addColumn({ type: 'date', id: 'End' });

    Object.entries(graphData).forEach(([deviceId, arr]) => {
        let newArr = [...arr];

        //add endpoints for orphans
        newArr = newArr.map((it) => {
            if(!it.start) {
                return {...it, start: min }
            }
            else if(!it.end) return {...it, end: now}
            return it;
        });

        //add endpoint buffers
        if(newArr.length == 0) {
            newArr.push({ start: min, end: min });
            newArr.push({ start: now, end: now });
        } else {
            if(newArr[0].start != min) newArr.push({ start: min, end: min });
            if(newArr[newArr.length - 1].end != now) newArr.push({ start: now, end: now });
        }

        let name;
        Object.entries(subscriptions).forEach(([key, val]) => {
            //if we are subscribed to this certain type
            if(val) {
                let found = val.find(it => it.id === deviceId);
                if(found) name = found.displayName;
            }
        });

        dataTable.addRows(newArr.map((parsed) => [name, moment(parsed.start).toDate(), moment(parsed.end).toDate()]));
    });

    

    let chart = new google.visualization.Timeline(document.getElementById("timeline"));
    chart.draw(dataTable, options.graphOptions);
}
        </script>
      </head>
      <body style="${fullSizeStyle}">
          <div id="timeline" style="${fullSizeStyle}"></div>
      </body>
    </html>
    """
    
return html;
}

// Create a formatted date object string for Google Charts Timeline
def getDateString(date) {
    def dateObj = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", date.toString())
    //def dateObj = date
    def year = dateObj.getYear() + 1900
    def dateString = "new Date(${year}, ${dateObj.getMonth()}, ${dateObj.getDate()}, ${dateObj.getHours()}, ${dateObj.getMinutes()}, ${dateObj.getSeconds()})"
    dateString
}

// Events come in Date format
def getDateStringEvent(date) {
    def dateObj = date
    def yyyy = dateObj.getYear() + 1900
    def MM = String.format("%02d", dateObj.getMonth()+1);
    def dd = String.format("%02d", dateObj.getDate());
    def HH = String.format("%02d", dateObj.getHours());
    def mm = String.format("%02d", dateObj.getMinutes());
    def ss = String.format("%02d", dateObj.getSeconds());
    def dateString = /$yyyy-$MM-$dd $HH:$mm:$ss.000/;
    dateString
}
    
def initializeAppEndpoint() {
    if (!state.endpoint) {
        try {
            def accessToken = createAccessToken()
            if (accessToken) {
                state.endpoint = getApiServerUrl()
                state.localEndpointURL = fullLocalApiServerUrl("")  
                state.remoteEndpointURL = fullApiServerUrl("")
                state.endpointSecret = accessToken
            }
        }
        catch(e) {
            log.debug("Error: $e");
            state.endpoint = null
        }
    }
    return state.endpoint
}

def getColorCode(code){
    switch (code){
        case "Maroon":  ret = "#800000"; break;
        case "Red":	    ret = "#FF0000"; break;
        case "Orange":	ret = "#FFA500"; break;	
        case "Yellow":	ret = "#FFFF00"; break;	
        case "Olive":	ret = "#808000"; break;	
        case "Green":	ret = "#008000"; break;	
        case "Purple":	ret = "#800080"; break;	
        case "Fuchsia":	ret = "#FF00FF"; break;	
        case "Lime":	ret = "#00FF00"; break;	
        case "Teal":	ret = "#008080"; break;	
        case "Aqua":	ret = "#00FFFF"; break;	
        case "Blue":	ret = "#0000FF"; break;	
        case "Navy":	ret = "#000080"; break;	
        case "Black":	ret = "#000000"; break;	
        case "Gray":	ret = "#808080"; break;	
        case "Silver":	ret = "#C0C0C0"; break;	
        case "White":	ret = "#FFFFFF"; break;
        case "Transparent": ret = "transparent"; break;
    }
}

//oauth endpoints
def getGraph() {
    return render(contentType: "text/html", data: getTimeLine());      
}

def getData() {
    def timeline = buildData();
    
    def formatEvents = [:];
    
    timeline.each{device->
        formatEvents[device.id_] = [];
        device.events_.each{event->
            formatEvents[device.id_] << ["start": event.start, "end": event.end];
        }
    }
        
    return render(contentType: "text/json", data: JsonOutput.toJson(formatEvents));
}

def getOptions() {
    return render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def subscriptions = [
        switches: switches,
        motions: motions,
        contacts: contacts,
        presences: presences
    ]
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
