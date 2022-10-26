package metro;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class Transfer {
    private String line;
    private String station;
    private int cost = 5;

    public Transfer(String line, String station) {
        this.line = line;
        this.station = station;
    }

    public String getLine() {
        return line;
    }

    public String getStation() {
        return station;
    }
    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return " - " + station + " (" + line + " line)";
    }
}
class Station {
    private String name;
    private boolean visited;
    private int distance = 0;
    private int time;
    private String line;

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    private List<Transfer> transfer = new ArrayList<>();
    private List<String> prev = new ArrayList<>();
    private List<String> next = new ArrayList<>();

    public Station(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTransfer(Transfer transition) {
        transfer.add(transition);
    }

    public boolean hasTransfer() {
        return !transfer.isEmpty();
    }

    public List<Transfer> getTransfer() {
        return transfer;
    }
    public List<String> getPast(){return prev;}
    public List<String> getNext(){return next;}

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
    public int getTime() {
        return time;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toStringWithTransfer() {
        if (transfer.size() != 0) {
            return name + transfer.get(0);
        } else {
            return name;
        }
    }
}
public class Main {

    public static Map<String, ArrayList<Station>> data;
    public static String[] requerement;
    public static Map<String, LinkedList<Station>> metro_data = new HashMap<>();
    public static Map<Station, Station> previous = new HashMap<>();
    public static Map<Station, Integer> costs = new HashMap<>();
    public static Map<Station, Station> parents = new HashMap<>();
    public static void FromFile(String dirpath)
    {
        Type type = new TypeToken<HashMap<String, ArrayList<Station>>>() {
        }.getType();
        Gson gson = new Gson();
        try {
            JsonReader reader = new JsonReader(new FileReader(dirpath));
            data = gson.fromJson(reader, type);
            for (var entry : data.entrySet()) {
                String lineName = entry.getKey();
                LinkedList<Station> stationsList = new LinkedList<>();
                entry.getValue().forEach((station) -> {
                    station.setLine(lineName);
                    stationsList.add(station);
                });
                metro_data.put(lineName, stationsList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void run(String[] req) {
        switch (req[0]) {
            case "/output":
                LinkedList<Station> lineStations = metro_data.get(req[1]);
                if (lineStations.isEmpty()) {
                    return;
                }
                System.out.println("depot");
                lineStations.forEach(station -> {
                    System.out.println(station.toStringWithTransfer());
                });
                System.out.println("depot");
                break;
            case "/append":
                var line = metro_data.get(req[1]);
                Station station = new Station(req[2]);
                line.add(line.size(), station);
                metro_data.put(req[1], line);
                break;
            case "/remove":
                line = metro_data.get(req[1]);
                for (int i = 0; i < line.size(); i++) {
                    if (line.get(i).getLine().equals(req[2])) {
                        line.remove(i);
                        break;
                    }
                }
                metro_data.put(req[1], line);
                break;
            case "/add-head":
                line = metro_data.get(req[1]);
                station = new Station(req[2]);
                line.addFirst(station);
                metro_data.put(req[1], line);
                break;
            case "/connect":
                findStation(req[1], req[2]).setTransfer(new Transfer(req[3], req[4]));
                findStation(req[3], req[4]).setTransfer(new Transfer(req[1], req[2]));
                break;
            case "/route":
                resetVisited();
                LinkedList<Station> queue = new LinkedList<>();
                LinkedList<Station> path = new LinkedList<>();
                Station first = null;
                queue.add(findStation(req[1], req[2]));
                while (!queue.isEmpty()) {
                    first = queue.get(0);
                    queue.remove(0);
                    if (!first.isVisited()) {
                        if (first == findStation(req[3], req[4])) {
                            break;
                        } else {
                            var neighbours = find_neighbours(first);
                            for (Station s : neighbours) {
                                previous.put(s, first);
                                if (s == findStation(req[3], req[4])) {
                                    first = s;
                                    queue.clear();
                                    queue.add(s);
                                    break;
                                }
                                queue.add(s);
                            }
                            first.setVisited(true);
                        }
                    }
                }
                path.add(first);
                while (previous.get(first) != null) {
                    first = previous.get(first);
                    path.add(first);
                }
                LinkedList<Station> path_reverse = new LinkedList<>();
                for (int i = path.size() - 1; i >= 0; i--) {
                    path_reverse.add(path.get(i));
                }
                LinkedList<Station> path_reverse1 = new LinkedList<>(path_reverse);
                for (int i = 0; i < path_reverse1.size() - 2;i++) {
                    var transfers0 = path_reverse1.get(i).getTransfer();
                    var transfer2 = path_reverse1.get(i + 2);
                    for (Transfer tr : transfers0) {
                        if (findStation(tr.getLine(), tr.getStation()) == transfer2) {
                            path_reverse.remove(i + 1);
                        }
                    }
                }
                Iterator<Station> iterator = path_reverse.iterator();
                String line1 = req[1];
                while(iterator.hasNext()){
                    Station station1 = iterator.next();
                    if ( ! line1.equals(station1.getLine())) {
                        line1 = station1.getLine();
                        System.out.println("Transition to line " + line1);
                    }
                    System.out.println(station1.getName());
                }
                break;
            case "/fastest-route":
                resetVisited();
                Station start_node = findStation(req[1], req[2]);
                Station last_node = findStation(req[3], req[4]);
                LinkedList<Station> neighbours = find_neighbours(start_node);
                start_node.setVisited(true);
                Station node = find_low_time_station(costs);
                while (node != null) {
                    int time = costs.get(node);
                    neighbours = find_neighbours(node);
                    node.setVisited(true);
                    node = find_low_time_station(costs);
                }
                LinkedList<Station> path0 = new LinkedList<>();
                Station first0 = last_node;
                path0.add(first0);
                while (parents.get(first0) != null) {
                    first0 = parents.get(first0);
                    path0.add(first0);
                }
                LinkedList<Station> path_reverse0 = new LinkedList<>();
                for (int i = path0.size() - 1; i >= 0; i--) {
                    path_reverse0.add(path0.get(i));
                }
                path_reverse1 = new LinkedList<>(path_reverse0);
                for (int i = 0; i < path_reverse1.size() - 2;i++) {
                    var transfers0 = path_reverse1.get(i).getTransfer();
                    var transfer2 = path_reverse1.get(i + 2);
                    for (Transfer tr : transfers0) {
                        if (findStation(tr.getLine(), tr.getStation()) == transfer2) {
                            path_reverse0.remove(i + 1);
                        }
                    }
                }
                Iterator<Station> iterator0 = path_reverse0.iterator();
                String line0 = req[1];
                while(iterator0.hasNext()){
                    Station station1 = iterator0.next();
                    if ( ! line0.equals(station1.getLine())) {
                        line0 = station1.getLine();
                        System.out.println("Transition to line " + line0);
                    }
                    System.out.println(station1.getName());
                }
                System.out.println("Total: " + costs.get(last_node) + " in the way");
                break;
        }
    }
    public static Station find_low_time_station(Map<Station, Integer> costs) {
        int lowest_time = 1000;
        Station lowest_time_station = null;
        for (var entry : costs.entrySet()) {
            int time = costs.get(entry.getKey());
            if (time < lowest_time && !entry.getKey().isVisited()) {
                lowest_time = time;
                lowest_time_station = entry.getKey();
            }
        }
        return lowest_time_station;
    }
    public static void resetVisited() {
        metro_data.forEach((lineName, stations) -> {
            stations.forEach(station -> {
                station.setVisited(false);
                station.setDistance(0);
            });
        });
    }
    public static Station findStation(String lineName, String stationName) {
        LinkedList<Station> stations = metro_data.get(lineName);
        for (Station station : stations) {
            if (station.getName().equals(stationName)) {
                return station;
            }
        }
        return null;
    }
    public static LinkedList<Station> find_neighbours(Station current) {
        LinkedList<Station> neighbours = new LinkedList<>();
        List<Station> stations = metro_data.get(current.getLine());
        for (Transfer transfer : current.getTransfer()) {
            Station search = findStation(transfer.getLine(), transfer.getStation());
            if (!search.isVisited()) {
                neighbours.add(search);
                int time1 = 1000;
                if (costs.get(search) != null) {
                    time1 = costs.get(search);
                }
                int time = 0;
                if (costs.get(current) != null) {
                    time = costs.get(current);
                }
                if (time1 > time + 5) {
                    costs.put(search, time + 5);
                    parents.put(search, current);
                }
            }
        }
        if (current.getPast() != null) {
            for (String prev : current.getPast()) {
                Station search = findStation(current.getLine(), prev);
                if (!search.isVisited()) {
                    neighbours.add(search);
                    int time = 0;
                    int time1 = 0;
                    if (costs.get(current) == null) {
                        time = 0;
                    } else {
                        time = costs.get(current);
                    }
                    if (costs.get(search) == null) {
                        time1 = 1000;
                    } else {
                        time1 = costs.get(search);
                    }
                    if (time1 > search.getTime() + time) {
                        costs.put(search, search.getTime() + time);
                        parents.put(search, current);
                    }
                }
            }
        }
        if (current.getNext() != null) {
            for (String next : current.getNext()) {
                Station search = findStation(current.getLine(), next);
                if (!search.isVisited()) {
                    neighbours.add(search);
                    int time = 0;
                    int time1 = 0;
                    if (costs.get(current) == null) {
                        time = 0;
                    } else {
                        time = costs.get(current);
                    }
                    if (costs.get(search) == null) {
                        time1 = 1000;
                    } else {
                        time1 = costs.get(search);
                    }
                    if (time1 > current.getTime() + time) {
                        costs.put(search, current.getTime() + time);
                        parents.put(search, current);
                    }
                }
            }
        }
        return neighbours;
    }
    public static void get_requirement(String scan) {
        String[] commands = scan.split(" ");
        int i = 0;
        requerement = new String[6];
        int k = 0;
        while (k < commands.length) {
            if (k + 1 == commands.length) {
                requerement[i] = commands[k].replaceAll("\"", "");
                break;
            }
            if (commands[k].startsWith("\"")) {
                if (commands[k].startsWith("\"") && commands[k].endsWith("\"")) {
                    requerement[i] = commands[k].replaceAll("\"", "");
                    k++;
                    i++;
                } else {
                    requerement[i] = commands[k].replaceAll("\"", "");
                    if (k + 1 >= commands.length) {
                        break;
                    }
                    int nach = k;
                    k++;
                    for (int j = nach + 1; j < commands.length; j++) {
                        if (!commands[j].endsWith("\"")) {
                            requerement[i] += " " + commands[j];
                            k++;
                        } else {
                            requerement[i] += " " + commands[j].replaceAll("\"", "");
                            k++;
                            i++;
                            break;
                        }
                    }
                }
            } else {
                requerement[i] = commands[k];
                i++;
                k++;
            }
        }
    }
    public static void main(String[] args) {
        Scanner dscanner = new Scanner(System.in);
        String dirpath = "./test/london.json";
        if (dirpath.contains("./test/prague.json")) {
            dirpath = "C:\\Users\\Sveta\\IdeaProjects\\prague_correct.json";
        }
        if (dirpath.contains("./test/london.json")) {
            dirpath = "C:\\Users\\Sveta\\Downloads\\london.json";
        }
        FromFile(dirpath);
        if (metro_data != null) {
            String scan = dscanner.nextLine().trim();
            get_requirement(scan);
            while (!scan.equals("/exit")) {
                run(requerement);
                scan = dscanner.nextLine().trim();
                get_requirement(scan);
            }
        }
    }
}