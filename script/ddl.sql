/*
Script para la creaci�n de la bd.
*/

--Tabla de los usuarios. Todos.
create table usuarios(
id integer, --Identificador �nico del usuario
idcreador integer, --Id del administrador que lo crea
idcolegio integer, --Colegio asociado al ni�o
nombreusuario varchar(25), --Nombre que ser� mostrado a los usuarios 
clave varchar(150), --Clave codificada de un usuario
email varchar(320), --Debe ser �nico, sirve para logearse
nombrereal varchar(150),
nombrenino varchar(50), --Para mostrar como padre de ...
sexotutor integer, -- 0 para hombre, 1 para mujer
sexonino integer,
direccion varchar(150),
localidad varchar(50),
provincia varchar(50),
codigopostal integer,
latitud numeric(12,8),
longitud numeric(12,8),
parentesco integer, --Padre/Madre 0, abuelo 1, tio 2, Primo 3, Hermano 4, Amigo del padre 5, tutor legal 6
fnacimientopadre date,
fnacimientonino date,
nacionalidadpadre varchar(50),
nacionalidadhijo varchar(50),
profesion varchar(25),
confirmado integer, --0 si no ha confimado su email, 1 si lo ha confirmado
activo integer, --0 si el usuario no es utilizable, 1 si el usuario es funcional (por defecto)
fechaCreacion date,
alertaMail integer
);

--Tabla de usuarios administradores
create table administradores(
id integer--Los ids en esta tabla ser�n administradores. Corresponde a ids de usuarios normales.
);

--Tabla para los lugares de inter�s. Posibles destinos
create table lugares(
id integer,--Id �nico para el lugar en la bd
tipo integer, -- 0 colegio, 1 monitor, 3 instituto, 4 guarder�a, 5 universidad, 6 museo, 7 polideportivo, 8 parque, 9 otros
nombre varchar(100), --Nombre que se mostrar�
latitud numeric(12,8),
longitud numeric(12,8)
);

--Tabla donde se almacenan iconos con advertencias para mostrar en el mapa
create table iconos(
id integer,
idcreador integer, -- -1 si es p�blico, -2 si un usuario privado lo propone para p�blico y espera aceptaci�n, id del usuario creador si es privado
tipo integer, --Grado de alerta
comentario varchar(2000), --Comentario que se podr� leer.
latitud numeric(12,8),
longitud numeric(12,8)-- De 0 a 20 pone la seguridad al m�nimo, de 21 a 100 la pone al m�ximo, desde 101 solo informa.
--0 C cortada, 1 acera intransitable, 2 Trabajos en la v�a, 3 t. en acera, 4 andamiaje en la acera, 5 calle cortada
--25 policia, 30 comercio adherido,
-100 monitor colegio
);

--Tabla con los valores de seguridad personalizados para tramos
create table seguridad(
id integer, --Id �nico en la bd. Aconsejable el de graphopper.
fechaActualizacion date, 
idcreador integer, --Id del que introdujo el dato
privado integer, -- 0 si es p�blico, 1 si es un dato privado del creador
elevar integer, -- 0 si no se desea mandar a administrador, 1 si se desea compartir en p�blico.
latitud1 numeric(12,8), --Los dos nodos que componen el tramo
longitud1 numeric(12,8),
latitud2 numeric(12,8),
longitud2 numeric(12,8),
densidadtrafico integer,
acera integer,
anchocalle integer,
indicecriminalidad integer,
ratioaccidentes integer,
superficieadoquines integer, --0 si falso, 1 si verdadero
superficielisa integer, -- 0 si false, 1 si verdadero
pasopeatones integer, 
semaforos integer,
residencialcomercial integer, --Indica el movimiento de personas de la calle
conservacionedificios integer,
niveleconomico integer,
iluminacion integer,
velocidadmaxima integer,
callepeatonal integer, -- 0 si no es peatonal, 1 si tiene preferencia peatonal
carrilbici integer, -- 0 si no es carril bici, 1 si es o tiene carril bici
calidadcarrilbici integer, 
separacioncalzadaacera integer, -- 0 falso, 1 verdadero
aparcamientoacera integer, --idem
badenes integer, --idem
radar integer, --idem
pendiente integer, 
confort integer, --Indica el balance entre diversos factores como el ruido, contaminaci�n, zonas de ocio, de paseo...
polic�a integer -- 0 falso, 1 verdadero
);

create table propiedades(
clave varchar(50),
valor varchar(5000)
);

create table rutas(
id int,
fecha date,
kml varchar(500000), --Xml del kml de la ruta
instrucciones varchar(50000) 
);

create table alertas(
id int,
idcreador int,
titulo varchar(50),
texto varchar(250),
fechacreacion date,
fechavalidez date
);

create table foro{
id int, 
idpadre int, -- -1 sin padre, sino id del padre
idautor int,
fecha date,
titulo varchar(50),
mensaje varchar(5000)
}

