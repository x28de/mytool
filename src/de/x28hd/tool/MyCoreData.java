package de.x28hd.tool;

import java.util.Collection;
import java.util.LinkedList;

public class MyCoreData  implements MapItems {
	LinkedList<Object> linkedList;
	
	public MyCoreData() {
		linkedList = new LinkedList<Object>();
		linkedList.add("Mercury\tPlanet #1");
		linkedList.add("Venus\tPlanet #2");
		linkedList.add("Earth\tPlanet #3");
		linkedList.add("JavaSoft is not a Planet but a loooooooooooooooooong story");
		linkedList.add("Mars\tPlanet #4");
		linkedList.add("Jupiter\tPlanet #5");
		linkedList.add("Saturn\tPlanet #6");
		linkedList.add("Uranus\tPlanet #7");
		linkedList.add("Neptune\tPlanet #8");
		linkedList.add("Pluto\tNo longer a Planet");
	}

	public Collection<Object> getList() {
		return (Collection<Object>) linkedList;
	}
}
