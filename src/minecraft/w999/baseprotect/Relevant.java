package w999.baseprotect;

//Relevant: Used to cache whether or not an interactor is in the BaseProtect list of interactors to check
//This means an object only has to lookup itself in the list once per instance instead of for every check
public class Relevant{
	int age = -1; //Age increases whenever BaseProtect reloads. If age is not the same, deemed as unknown and require lookup.
	boolean relevant = false; //Set to true if relevant
}
