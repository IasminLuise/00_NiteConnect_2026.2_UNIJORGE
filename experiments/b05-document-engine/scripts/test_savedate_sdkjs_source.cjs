"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const sourceRoot = process.env.SDKJS_SOURCE || path.resolve(__dirname, "../../../.private/onlyoffice-sdkjs-source");
const instructionPath = path.join(sourceRoot, "word/Editor/Paragraph/ComplexFieldInstruction.js");
const complexFieldPath = path.join(sourceRoot, "word/Editor/Paragraph/ComplexField.js");
const fieldPath = path.join(sourceRoot, "word/Editor/Field.js");
const pagesCountPath = path.join(sourceRoot, "word/Editor/Paragraph/RunContent/PagesCount.js");
const serializerPath = path.join(sourceRoot, "word/Editor/Serialize2.js");

const fieldNames = [
	"UNKNOWN", "MERGEFIELD", "PAGENUM", "PAGECOUNT", "FORMTEXT", "TOC", "PAGEREF", "ASK",
	"REF", "HYPERLINK", "TIME", "DATE", "FORMULA", "SEQ", "STYLEREF", "NOTEREF", "ADDIN",
	"FORMCHECKBOX", "FORMDROPDOWN", "SAVEDATE"
];
const word = {
	FieldInstructionBase: function FieldInstructionBase() {
		this.InstructionLine = "";
		this.generalSwitches = 0;
		this.generalNumeric = -1;
	}
};
fieldNames.forEach((name, index) => { word[`fieldtype_${name}`] = "SAVEDATE" === name ? 0x50 : index; });
word.FieldInstructionBase.prototype = {
	SetInstructionLine(value) { this.InstructionLine = value; },
	addGeneralSwitches(values) { this.generalSwitches |= values.includes("MERGEFORMAT") ? 1 : 0; },
	isMergeFormat() { return !!(this.generalSwitches & 1); }
};

const cultureInfos = {
	1033: {Name: "en-US", DateSeparator: "/", UseAMPM: true},
	1046: {Name: "pt-BR", DateSeparator: "/", UseAMPM: false}
};
const formatCache = {
	get(picture) {
		if (picture === "INVALID") return null;
		return {formatToWord(serial, _precision, culture) { return `${culture.Name}|${picture}|${serial}`; }};
	}
};
const common = {
	g_aCultureInfos: cultureInfos,
	oNumFormatCache: formatCache,
	NumFormatType: {WordFieldDate: 1},
	getShortDateFormat(culture) { return "en-US" === culture.Name ? "M/d/yyyy" : "d/M/yyyy"; },
	getLongTimeFormat(culture) { return culture.UseAMPM ? "h:mm:ss AM/PM" : "h:mm:ss"; }
};
const context = {AscWord: word, Asc: {c_oAscNumberingFormat: {}}, AscCommon: common, window: {AscWord: word}};
vm.createContext(context);
vm.runInContext(fs.readFileSync(instructionPath, "utf8"), context, {filename: instructionPath});

const parser = new context.CFieldInstructionParser();
const instructionLine = 'SAVEDATE \\@ "dd/MM/yyyy HH:mm" \\* MERGEFORMAT';
const savedate = parser.GetInstructionClass(instructionLine);
assert.equal(savedate.Type, word.fieldtype_SAVEDATE);
assert.equal(savedate.GetFormat(), "dd/MM/yyyy HH:mm");
assert.equal(savedate.InstructionLine, instructionLine);
assert.equal(savedate.Unsupported, false);
assert.equal(savedate.isMergeFormat(), true);

const cased = parser.GetInstructionClass('SAVEDATE \\@ "d MMMM yyyy" \\* Upper');
assert.equal(cased.TextCase, "UPPER");
assert.equal(cased.Unsupported, false);

const unsupported = parser.GetInstructionClass("SAVEDATE \\# 0");
assert.equal(unsupported.Type, word.fieldtype_SAVEDATE);
assert.equal(unsupported.Unsupported, true);

const dateControl = parser.GetInstructionClass('DATE \\@ "yyyy-MM-dd"');
assert.equal(dateControl.Type, word.fieldtype_DATE);
assert.equal(dateControl.GetFormat(), "yyyy-MM-dd");

const complexSource = fs.readFileSync(complexFieldPath, "utf8");
const calculateStart = complexSource.indexOf("AscWord.calculateSavedate = function");
const calculateEnd = complexSource.indexOf("\nCComplexField.prototype.private_InsertError", calculateStart);
assert.notEqual(calculateStart, -1, "calculateSavedate source function exists");
assert.notEqual(calculateEnd, -1, "calculateSavedate source function terminates");
vm.runInContext(complexSource.slice(calculateStart, calculateEnd), context, {filename: complexFieldPath});

const savedAt = new Date("2026-10-25T20:02:19Z");
const doc = {Core: {asc_getModified() { return savedAt; }}, GetDefaultLanguage() { return 1033; }};
const ptRun = {GetDirectTextPr() { return {Lang: {Val: 1046}}; }};
const calculatedPt = word.calculateSavedate(savedate, doc, ptRun);
assert.equal(calculatedPt.ok, true);
assert.equal(calculatedPt.language, 1046, "run language takes precedence over document language");
assert.equal(calculatedPt.picture, "dd/MM/yyyy HH:mm");

const defaultFormat = parser.GetInstructionClass("SAVEDATE");
const calculatedEn = word.calculateSavedate(defaultFormat, doc, null);
assert.equal(calculatedEn.ok, true);
assert.equal(calculatedEn.language, 1033);
assert.match(calculatedEn.picture, /^M\/d\/yyyy h:mm:ss AM\/PM$/);

assert.equal(word.calculateSavedate(unsupported, doc, null).reason, "UNSUPPORTED_SWITCH");
assert.equal(word.calculateSavedate(parser.GetInstructionClass('SAVEDATE \\@ "INVALID"'), doc, null).reason, "UNSUPPORTED_PICTURE");
assert.equal(word.calculateSavedate(defaultFormat, {Core: {asc_getModified() { return null; }}}, null).reason, "MISSING_LAST_SAVE");
assert.deepEqual(JSON.parse(JSON.stringify(word.savedateFormatSegments("25 October 2026", savedate, {marker: "value"}, {marker: "code"}))), [
	{text: "25", textPr: {marker: "value"}},
	{text: " October 2026", textPr: {marker: "code"}}
]);

const fieldSource = fs.readFileSync(fieldPath, "utf8");
assert.match(fieldSource, /case AscWord\.fieldtype_SAVEDATE\s*:\s*\{\s*name = "SAVEDATE";/);
assert.match(fieldSource, /AscWord\.calculateSavedate\(this\.GetInstruction\(\), this\.Paragraph\.GetLogicDocument\(\), this\.GetFirstRunNonEmpty\(\)\)/);
assert.match(complexSource, /case AscWord\.fieldtype_SAVEDATE:\s*this\.private_UpdateSAVEDATE\(\);/);
assert.match(complexSource, /case AscWord\.fieldtype_SAVEDATE:\s*var savedate = AscWord\.calculateSavedate/);

const pageCountSource = fs.readFileSync(pagesCountPath, "utf8");
const serializerSource = fs.readFileSync(serializerPath, "utf8");
assert.match(pageCountSource, /CRunPagesCount\.prototype\.Copy = function\(\)/);
assert.match(pageCountSource, /copy\.fieldInstruction = this\.fieldInstruction/);
assert.match(fieldSource, /pageCountField\.fieldInstruction = this\.GetInstructionLine\(\)/);
assert.match(serializerSource, /elem\.pageNum\.fieldInstruction \|\| "NUMPAGES \\\\\* MERGEFORMAT"/);

word.CRunPageNum = function CRunPageNum() {
	this.pageNum = 5;
	this.graphemes = ["5"];
	this.widths = [7];
};
word.CRunPageNum.prototype.SetValue = function SetValue(value) { this.pageNum = value; };
context.para_PageCount = 9;
context.Asc.c_oAscNumberingFormat.Decimal = 0;
vm.runInContext(fs.readFileSync(pagesCountPath, "utf8"), context, {filename: pagesCountPath});
const pageCount = new word.CRunPagesCount(5);
pageCount.numFormat = 3;
pageCount.numText = "five";
pageCount.textPr = {marker: "text"};
pageCount.graphemes = ["V"];
pageCount.widths = [9];
pageCount.Width = 10;
pageCount.WidthVisible = 8;
pageCount.fieldInstruction = "NUMPAGES \\* Arabic";
const copiedPageCount = pageCount.Copy();
assert.equal(copiedPageCount.fieldInstruction, "NUMPAGES \\* Arabic");
assert.deepEqual(copiedPageCount.graphemes, ["V"]);
assert.deepEqual(copiedPageCount.widths, [9]);
assert.notEqual(copiedPageCount.graphemes, pageCount.graphemes);
assert.notEqual(copiedPageCount.widths, pageCount.widths);

console.log("SAVEDATE parser, locale, switch rejection, F9 routing, and NUMPAGES source checks: PASS");
