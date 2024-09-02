import { type ClassValue, clsx } from "clsx";
import round from "lodash/round";
import isObject from "lodash/isObject";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const isValidJsonObject = (string: string) => {
  let json = null;
  try {
    json = JSON.parse(string);
  } catch (e) {
    return false;
  }

  return json && isObject(json);
};

export const safelyParseJSON = (string: string) => {
  try {
    return JSON.parse(string);
  } catch (e) {
    console.error(e);
    return {};
  }
};

export const calcDuration = (start: string, end: string) => {
  return new Date(end).getTime() - new Date(start).getTime();
};

export const millisecondsToSeconds = (milliseconds: number) => {
  // rounds with precision, one character after the point
  return round(milliseconds / 1000, 1);
};
