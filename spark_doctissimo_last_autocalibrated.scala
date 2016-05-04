// General purpose libraries
import scala.xml._
import scala.util.matching.Regex

// Spark data manipulation libraries
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Row

// Spark machine learning libraries
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.tuning.{ParamGridBuilder, CrossValidator}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics


val fileName = "data_stripped_javaed_forSpark.xml"

// On charge le fichier texte (qui est dans le même dossier)
val textFile = sc.textFile(fileName)

// Le map va créer une trame de données avec chaque ligne du fichier XML
val postsXml = textFile.map(_.trim).
  filter(!_.startsWith("<?xml version=")).    //On garde ce qui ne commence PAS par ...
  filter(!_.startsWith("<forum")).
  filter(_ != "</forum>")       //On garde ce qui n'est PAS ...

// On va prendre ce qui nous intéresse dans les lignes ...
val postsRDD = postsXml.map { s =>
  val xml = XML.loadString(s)

  // On va récupérer chaque champ de la balise XML et le mettre dans une variable
  val id = (xml \ "@id").text
  val titleBrut = (xml \ "@title").text
  val messageBrut = (xml \ "@message").text
  val date = (xml \ "@date").text

  val message = ("<\\S+>".r).replaceAllIn(messageBrut, " ").replaceAll("\n", " ").replaceAll("( )+", " ");

  val title = ("<\\S+>".r).replaceAllIn(titleBrut, " ").replaceAll("\n", " ").replaceAll("( )+", " ");

  val tagsBrut = "<" + title.replaceAll(" ", "><") + ">";

  // enlever les balises contentant des mots de moins de 3 caractères
  val tags = ("<[a-zA-Z0-9]{1,2}>".r).replaceAllIn(tagsBrut, "");

  // Crée une ligne avec les valeurs obtenues
  Row(id, message, tags, date)
}


// ===== PREPARATION DE LA BASE D'ENTRAINEMENT =====

val schemaString = "id message tags date"

val schema = StructType(
  schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true))
)

val postsDf = sqlContext.createDataFrame(postsRDD, schema)

/*
println("\n===== RDD Display: postsDf =====")
postsDf.show()
*/

// ===== ANALYSE DES DONNEES DE LA BASE =====

// Choix du tag vérifier
val targetTag = "alimentation"

// Vérification de l'adéquation du tag ciblé avec chacun des messages
// myudf (User Defined Function): entrée: ligne (string): si contient "Alimentation" => 1.0 en sortie, 0.0 sinon (classifieur binaire)
val myudf: (String => Double) = (str: String) => {if (str.contains(targetTag)) 1.0 else 0.0}
val sqlfunc = udf(myudf)
val training = postsDf.withColumn("label", sqlfunc(col("tags")) )

println("\n===== RDD Display: training =====")
training.show()


// ===== CREATION DU MODELE =====
// https://spark.apache.org/docs/1.6.0/mllib-guide.html

// Configuration du Pipeline de ML (3 étapes : tokenizer, hashingTF, LinearRegression)
val tokenizer = new Tokenizer().setInputCol("message").setOutputCol("words")
val hashingTF = new HashingTF().setInputCol(tokenizer.getOutputCol).setOutputCol("features")
val lr = new LogisticRegression().setMaxIter(10)
val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, lr))

// ===== SELECTION DU MEILLEUR MODELE PAR "CROSS-VALIDATION" =====

val paramGrid = new ParamGridBuilder().addGrid(hashingTF.numFeatures, Array(1000, 5000, 10000, 50000, 100000)).addGrid(lr.regParam, Array(1.0, 0.1, 0.01, 0.001)).build()

val cv = new CrossValidator().setEstimator(pipeline).setEvaluator(new BinaryClassificationEvaluator ).setEstimatorParamMaps(paramGrid).setNumFolds(3)

// Création du PipelineModel
println("\n===== BEST MODEL FOUND =====")
val model = cv.fit(training)


// ===== TEST DU MODELE =====

val testTitle = "Alimentation équilibré, sucres : Je ne sais plus où j'en suis"
val testMessage = "Bonsoir ! Je ne sais par où commencer, mais j'ai extrêmement besoin d'aide... J'ai 17 ans, je fais actuellement 54,8 kilos et mesure 170. J'ai commencé un régime il y a quelques mois, et depuis, je suis enfermée dans un cercle vicieux : je compte mes calories, je culpabilise, je ne me sens pas bien dans ma peau. Au début, je faisais 60 kilos, j'étais bien même sans faire attention, tout en grignotant, en profitant de la vie, j'avais certes un peu de graisse abdominale, mais c'était surtout parce que je ne faisais pas de sport, et ça je ne l'avais pas compris avant... Aujourd'hui, je regrette d'avoir commencé tout ça, je fais actuellement 54,8, je me prive et fais du sport que je n'apprécie pas. Je suis malheureuse. Ma famille s'est énormément inquiété la semaine passé, j'étais faible, je tournais autour de 500 / 800 calories par jour, et je paniquais dès que j'osais manger plus... Mais, lundi dernier, je me suis dit que ça ne pouvait plus durer, et j'ai recommencé doucement à racheter des choses que j'aime. Mais, au bout de quelques jours, tout est tombé en ruine. Je faisais environ 53 kilos lundi dernier, et maintenant, me voilà à 54,8. Je grossis de jour en jour, et pourtant je ne grignote plus et mange plus équilibré qu'au début... Je ne sais plus quoi faire... Ces quatre derniers jours il est vrai que j'ai eu quelques excès, parce que repas de famille, sorties entre amies, mais rien de grave (enfin c'est ce que je pensais)... Je suis désolé, je me mélange un peu, mais j'ai tellement de choses à dire et tellement de choses que j'aimerai comprendre... En fait, j'ai téléchargé une nouvelle application de calories récemment, avant je ne regardais que les calories et pas le reste : pas le sucres par exemple... Maintenant oui, et je panique. 25g de sucres à 50g de sucres par jour ? Au délà de l'excès ? Mais depuis combien de temps, je suis en excès alors ? Rien que de m'imaginer enlever ces quelques plaisirs à nouveau, ça me fait peur... En fait, pour mieux vous illustrer ça, voici une journée type de ce que je mange : Au petit déjeuner : un bol de lait écrémé (250 ml) avec une cuillère de Nesquik (8g), 60g de pain complet et 10g de beurre... Avant, je mangeai un fruit en plus, mais rien qu'avec ça, j'en suis déjà à 15g de sucre... En suite, à midi : j'essaye de varier, mais par exemple lundi, j'ai mangé 30g de pain complet, un steak haché 5% MG, un carré frais 0%, 145g de ratatouille et une pomme... Je pensais que ce n'était pas grand chose et pourtant, 18g de sucres en plus... J'adore manger un dessert après un repas, mais je me rend compte que c'est impossible... Sinon, trop de sucres en fin de journée, donc depuis je ne mange plus de fruits à midi ni de yaourt, du moins j'essaie. Après, au goûter, j'ai racheté quelques gâteaux que j'aime comme des petits beurres, des barquettes à la fraise, ou des oursons Lu, parce que je voulais me refaire plaisir... Mais impossible ! Trop de sucres à nouveau... Je ne vais finalement pas pouvoir les manger sans culpabiliser... Alors, je pensais quand même prendre un fruit ou un chocolat chaud, mais trop de sucres à nouveau... Une pomme 20g... Un chocolat chaud 10g... Et le soir, je mange généralement un bouillon ou une soupe, et parfois en plus, 80g de blanc d'oeufs et 150g de légumes + un yaourt ou un fruit... En suivant cela, j'arrive parfois à 60/70g de sucres, et avec des excès comme samedi, 80g... Quand j'ai su ça, alors que ce n'était juste qu'un smoothie... Je n'ai pas compris, et je ne sais plus quoi faire... Je tournai autour de 1400 calories depuis que je m'étais rendu compte que je me mettais en danger, mais à cause de ma baisse au niveau fruits / laitages, je ne suis plus qu'à 1000 calories... Vous trouvez que je mange trop de mauvaises choses ? Que mon alimentation n'est pas équilibré ? Je fais de mon mieux pour tout respecter, j'ai un peu du mal à retirer le goûter mais sans lui, mes apports caloriques baisseraient encore... D'ailleurs ça aussi, je ne sais toujours pas réellement combien dois-je consommer de calories par jour ? Vous l'aurez compris je suis perdue, j'avais déjà posté sur ce forum mais je n'avais jamais eu de réponses, et avec ce message si embrouillé, je doute que j'en aurais... Mais je ne sais vraiment pas où j'en suis... Je pensais pourtant que les fruits et les laitages n'étaient pas nos ennemis... Me serais-je tromper ? Je ne sais pas comment faire pour ne plus en manger, pour manger moins de sucres, je pense que je serais réellement frustrer... Enfin... Merci d'avance pour votre aide, bonne soirée !"
val testText = testTitle + " " + testMessage

val testDF = sqlContext.createDataFrame(Seq(
  (5678, testText, "<Alimentation><équilibré><sucres>", "Posté le 13/05/2016"),
  (2500, "The scala.MatchError  (of class org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema) exception happens when I try to access DataFrame row elements. The following code counts book pairs, where count of a pair equals the number of readers who read this pair of books.", "<scala><MatchError>", "Posté le 24/02/2016")
)).toDF("id", "message", "tags", "date")

println("\n===== RDD Display: testDF =====")
testDF.show()

// montrer les résultats pour les deux messages : le taux de vrai-positif, la probabilité de correspondance au tag choisi, et le label obtenu (0.0 ou 1.0)
val result = model.transform(testDF).select("id", "message", "tags", "date", "probability", "prediction").collect().foreach {
  case Row(id: Int, message: String, tags: String, date: String, prob: Vector, prediction: Double) => println(s"($id, $tags) --> prob=$prob, prediction=$prediction")
}
