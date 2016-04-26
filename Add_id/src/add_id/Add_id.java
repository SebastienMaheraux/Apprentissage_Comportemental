package add_id;
import java.io.*;
import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;
import java.util.List;
import java.util.Iterator;

public class Add_id
{
  static org.jdom2.Document doc;
  static Element root;

  public static void main(String[] args)
  {
    try
    {
      //Les fichiers .xml se trouve à la racine de projet
      readFile("test.xml");
      //System.out.println("rootElement : "+root);
      modifieElement("id");
      saveFile("data.xml");
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  //On parse le file et on initialise la racine de
  //notre arborescence
  static void readFile(String file) throws Exception
  {
    SAXBuilder sxb = new SAXBuilder();
    doc = sxb.build("test.xml");
    root = doc.getRootElement();
  }

  //On fait des modifications sur un Element
  static void modifieElement(String element)
  {
    //Dans un premier temps on liste tous les topics
    List listTopics = root.getChildren("topic");
    Iterator i = listTopics.iterator();
    int n = 1;
    //On parcours la liste grâce à un iterator
    while(i.hasNext())
    {
    Element courant = (Element)i.next();
    //Si le topic possède l'Element on applique
    //les modifications.
      if(courant.getAttribute(element)!=null)
      {
        courant.setAttribute(element, Integer.toString(n));
        n++;

      }
    }
  }

  //On enregistre notre nouvelle arborescence dans le fichier
  //d'origine dans un format classique.
  static void saveFile(String file) throws Exception
  {
    XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
    out.output(doc, new FileOutputStream(file));
  }
}
